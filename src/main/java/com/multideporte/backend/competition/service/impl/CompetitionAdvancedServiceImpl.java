package com.multideporte.backend.competition.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedBracketResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedCalendarResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedMatchSummary;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedResultsResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedTeamSummary;
import com.multideporte.backend.competition.service.CompetitionAdvancedService;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.match.repository.MatchGameSpecifications;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionAdvancedServiceImpl implements CompetitionAdvancedService {

    private static final Set<MatchGameStatus> CLOSED_MATCH_STATUSES = EnumSet.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT);

    private final TournamentRepository tournamentRepository;
    private final MatchGameRepository matchGameRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final StandingRepository standingRepository;

    @Override
    public CompetitionAdvancedBracketResponse getBracket(Long tournamentId, Long stageId) {
        Tournament tournament = findTournament(tournamentId);
        Map<Long, TournamentStage> stagesById = tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId).stream()
                .collect(Collectors.toMap(TournamentStage::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        TournamentStage bracketStage = resolveBracketStage(tournament.getId(), stageId, stagesById);
        List<MatchGame> stageMatches = fetchMatches(tournamentId, bracketStage.getId(), null, null).stream()
                .filter(match -> match.getGroupId() == null)
                .sorted(Comparator
                        .comparing(MatchGame::getRoundNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MatchGame::getMatchdayNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MatchGame::getScheduledAt, Comparator.nullsLast(OffsetDateTime::compareTo))
                        .thenComparing(MatchGame::getId))
                .toList();

        Map<Long, CompetitionAdvancedTeamSummary> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(stageMatches);
        List<CompetitionAdvancedMatchSummary> summaries = stageMatches.stream()
                .map(match -> toMatchSummary(match, stagesById, Map.of(), teamsByTournamentTeamId))
                .toList();

        Map<Integer, List<CompetitionAdvancedMatchSummary>> rounds = new LinkedHashMap<>();
        for (CompetitionAdvancedMatchSummary summary : summaries) {
            rounds.computeIfAbsent(summary.roundNumber(), ignored -> new ArrayList<>()).add(summary);
        }

        List<CompetitionAdvancedBracketResponse.BracketRound> roundResponses = rounds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(Integer::compareTo)))
                .map(entry -> new CompetitionAdvancedBracketResponse.BracketRound(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue()
                ))
                .toList();

        return new CompetitionAdvancedBracketResponse(
                tournamentId,
                bracketStage.getId(),
                bracketStage.getName(),
                bracketStage.getStageType().name(),
                summaries.size(),
                roundResponses
        );
    }

    @Override
    public CompetitionAdvancedCalendarResponse getCalendar(
            Long tournamentId,
            Long stageId,
            Long groupId,
            MatchGameStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        findTournament(tournamentId);
        validateStageAndGroup(tournamentId, stageId, groupId);

        Map<Long, TournamentStage> stagesById = tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId).stream()
                .collect(Collectors.toMap(TournamentStage::getId, Function.identity()));
        Map<Long, StageGroup> groupsById = loadGroupsById(stagesById.values());

        List<MatchGame> matches = fetchMatches(tournamentId, stageId, groupId, status).stream()
                .filter(match -> withinDateRange(match.getScheduledAt(), from, to))
                .sorted(Comparator
                        .comparing(MatchGame::getScheduledAt, Comparator.nullsLast(OffsetDateTime::compareTo))
                        .thenComparing(MatchGame::getRoundNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MatchGame::getMatchdayNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MatchGame::getId))
                .toList();

        Map<Long, CompetitionAdvancedTeamSummary> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(matches);
        List<CompetitionAdvancedMatchSummary> summaries = matches.stream()
                .map(match -> toMatchSummary(match, stagesById, groupsById, teamsByTournamentTeamId))
                .toList();

        int scheduledMatches = (int) matches.stream().filter(match -> match.getStatus() == MatchGameStatus.SCHEDULED).count();
        int closedMatches = (int) matches.stream().filter(match -> CLOSED_MATCH_STATUSES.contains(match.getStatus())).count();

        return new CompetitionAdvancedCalendarResponse(
                tournamentId,
                stageId,
                groupId,
                status,
                from,
                to,
                summaries.size(),
                scheduledMatches,
                closedMatches,
                summaries
        );
    }

    @Override
    public CompetitionAdvancedResultsResponse getResults(Long tournamentId, Long stageId, Long groupId) {
        findTournament(tournamentId);
        validateStageAndGroup(tournamentId, stageId, groupId);

        Map<Long, TournamentStage> stagesById = tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId).stream()
                .collect(Collectors.toMap(TournamentStage::getId, Function.identity()));
        Map<Long, StageGroup> groupsById = loadGroupsById(stagesById.values());

        List<MatchGame> closedMatches = fetchMatches(tournamentId, stageId, groupId, null).stream()
                .filter(match -> CLOSED_MATCH_STATUSES.contains(match.getStatus()))
                .sorted(Comparator
                        .comparing(MatchGame::getScheduledAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MatchGame::getId, Comparator.reverseOrder()))
                .toList();

        Map<Long, CompetitionAdvancedTeamSummary> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(closedMatches);
        List<CompetitionAdvancedResultsResponse.ResultEntry> results = closedMatches.stream()
                .map(match -> buildResultEntry(match, stagesById, groupsById, teamsByTournamentTeamId))
                .toList();

        return new CompetitionAdvancedResultsResponse(
                tournamentId,
                stageId,
                groupId,
                results.size(),
                results
        );
    }

    private Tournament findTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado con id: " + tournamentId));
    }

    private TournamentStage resolveBracketStage(
            Long tournamentId,
            Long stageId,
            Map<Long, TournamentStage> stagesById
    ) {
        if (stageId != null) {
            TournamentStage stage = stagesById.get(stageId);
            if (stage == null) {
                throw new ResourceNotFoundException("Stage no encontrado con id: " + stageId);
            }
            if (stage.getStageType() != TournamentStageType.KNOCKOUT) {
                throw new BusinessException("El bracket solo puede consultarse sobre una etapa KNOCKOUT");
            }
            return stage;
        }

        return stagesById.values().stream()
                .filter(stage -> stage.getStageType() == TournamentStageType.KNOCKOUT && Boolean.TRUE.equals(stage.getActive()))
                .findFirst()
                .or(() -> stagesById.values().stream()
                        .filter(stage -> stage.getStageType() == TournamentStageType.KNOCKOUT)
                        .filter(stage -> !fetchMatches(tournamentId, stage.getId(), null, null).isEmpty())
                        .findFirst())
                .or(() -> stagesById.values().stream()
                        .filter(stage -> stage.getStageType() == TournamentStageType.KNOCKOUT)
                        .findFirst())
                .orElseThrow(() -> new BusinessException("El torneo no tiene una etapa KNOCKOUT disponible para exponer llaves"));
    }

    private void validateStageAndGroup(Long tournamentId, Long stageId, Long groupId) {
        if (stageId == null && groupId != null) {
            throw new BusinessException("groupId requiere stageId dentro del contrato de competencia avanzada");
        }

        if (stageId != null) {
            TournamentStage stage = tournamentStageRepository.findById(stageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stage no encontrado con id: " + stageId));
            if (!Objects.equals(stage.getTournamentId(), tournamentId)) {
                throw new BusinessException("El stage indicado no pertenece al torneo solicitado");
            }
        }

        if (groupId != null) {
            StageGroup group = stageGroupRepository.findById(groupId)
                    .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado con id: " + groupId));
            if (!Objects.equals(group.getStageId(), stageId)) {
                throw new BusinessException("El grupo indicado no pertenece al stage solicitado");
            }
        }
    }

    private List<MatchGame> fetchMatches(Long tournamentId, Long stageId, Long groupId, MatchGameStatus status) {
        Specification<MatchGame> specification = MatchGameSpecifications.byFilters(tournamentId, stageId, groupId, status);
        return matchGameRepository.findAll(specification);
    }

    private Map<Long, StageGroup> loadGroupsById(Collection<TournamentStage> stages) {
        Map<Long, StageGroup> groupsById = new HashMap<>();
        for (TournamentStage stage : stages) {
            for (StageGroup group : stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(stage.getId())) {
                groupsById.put(group.getId(), group);
            }
        }
        return groupsById;
    }

    private Map<Long, CompetitionAdvancedTeamSummary> buildTeamsByTournamentTeamId(List<MatchGame> matches) {
        Set<Long> tournamentTeamIds = matches.stream()
                .flatMap(match -> java.util.Arrays.asList(
                        match.getHomeTournamentTeamId(),
                        match.getAwayTournamentTeamId(),
                        match.getWinnerTournamentTeamId()
                ).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (tournamentTeamIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, TournamentTeam> tournamentTeams = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Set<Long> teamIds = tournamentTeams.values().stream()
                .map(TournamentTeam::getTeamId)
                .collect(Collectors.toSet());
        Map<Long, Team> teams = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        Map<Long, CompetitionAdvancedTeamSummary> response = new HashMap<>();
        for (Long tournamentTeamId : tournamentTeamIds) {
            TournamentTeam tournamentTeam = tournamentTeams.get(tournamentTeamId);
            if (tournamentTeam == null) {
                continue;
            }

            Team team = teams.get(tournamentTeam.getTeamId());
            response.put(tournamentTeamId, new CompetitionAdvancedTeamSummary(
                    tournamentTeamId,
                    tournamentTeam.getTeamId(),
                    team != null ? team.getName() : null,
                    team != null ? team.getShortName() : null,
                    team != null ? team.getCode() : null,
                    tournamentTeam.getSeedNumber()
            ));
        }
        return response;
    }

    private CompetitionAdvancedMatchSummary toMatchSummary(
            MatchGame match,
            Map<Long, TournamentStage> stagesById,
            Map<Long, StageGroup> groupsById,
            Map<Long, CompetitionAdvancedTeamSummary> teamsByTournamentTeamId
    ) {
        TournamentStage stage = Optional.ofNullable(match.getStageId()).map(stagesById::get).orElse(null);
        StageGroup group = Optional.ofNullable(match.getGroupId()).map(groupsById::get).orElse(null);

        return new CompetitionAdvancedMatchSummary(
                match.getId(),
                match.getStageId(),
                stage != null ? stage.getName() : null,
                stage != null ? stage.getStageType().name() : null,
                match.getGroupId(),
                group != null ? group.getCode() : null,
                group != null ? group.getName() : null,
                match.getRoundNumber(),
                match.getMatchdayNumber(),
                match.getScheduledAt(),
                match.getVenueName(),
                match.getStatus(),
                match.getHomeScore(),
                match.getAwayScore(),
                teamsByTournamentTeamId.get(match.getHomeTournamentTeamId()),
                teamsByTournamentTeamId.get(match.getAwayTournamentTeamId()),
                teamsByTournamentTeamId.get(match.getWinnerTournamentTeamId()),
                match.getNotes()
        );
    }

    private CompetitionAdvancedResultsResponse.ResultEntry buildResultEntry(
            MatchGame match,
            Map<Long, TournamentStage> stagesById,
            Map<Long, StageGroup> groupsById,
            Map<Long, CompetitionAdvancedTeamSummary> teamsByTournamentTeamId
    ) {
        CompetitionAdvancedMatchSummary summary = toMatchSummary(match, stagesById, groupsById, teamsByTournamentTeamId);
        TournamentStage stage = Optional.ofNullable(match.getStageId()).map(stagesById::get).orElse(null);

        boolean affectsStandings = stage == null || stage.getStageType() != TournamentStageType.KNOCKOUT;
        String standingScope = resolveStandingScope(stage, match.getGroupId());
        String standingStatus = resolveStandingStatus(match, stage, affectsStandings);

        return new CompetitionAdvancedResultsResponse.ResultEntry(summary, affectsStandings, standingScope, standingStatus);
    }

    private String resolveStandingScope(TournamentStage stage, Long groupId) {
        if (stage != null && stage.getStageType() == TournamentStageType.KNOCKOUT) {
            return "NOT_APPLICABLE";
        }
        if (groupId != null) {
            return "GROUP";
        }
        if (stage != null) {
            return "STAGE";
        }
        return "TOURNAMENT";
    }

    private String resolveStandingStatus(MatchGame match, TournamentStage stage, boolean affectsStandings) {
        if (!affectsStandings) {
            return "NOT_APPLICABLE";
        }

        List<Standing> standings;
        if (match.getGroupId() != null) {
            standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(
                    match.getTournamentId(),
                    match.getStageId(),
                    match.getGroupId()
            );
        } else if (stage != null) {
            standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupId(
                    match.getTournamentId(),
                    match.getStageId(),
                    null
            );
        } else {
            standings = standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(match.getTournamentId());
        }

        return standings.isEmpty() ? "PENDING_RECALCULATION" : "AVAILABLE";
    }

    private boolean withinDateRange(OffsetDateTime scheduledAt, OffsetDateTime from, OffsetDateTime to) {
        if (from == null && to == null) {
            return true;
        }
        if (scheduledAt == null) {
            return false;
        }
        if (from != null && scheduledAt.isBefore(from)) {
            return false;
        }
        return to == null || !scheduledAt.isAfter(to);
    }
}
