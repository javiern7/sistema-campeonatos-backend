package com.multideporte.backend.publicportal.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedMatchSummary;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedCalendarResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedResultsResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedTeamSummary;
import com.multideporte.backend.competition.service.CompetitionAdvancedService;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.publicportal.dto.PublicMatchSummaryResponse;
import com.multideporte.backend.publicportal.dto.PublicPortalHomeResponse;
import com.multideporte.backend.publicportal.dto.PublicReadModulesResponse;
import com.multideporte.backend.publicportal.dto.PublicStandingEntryResponse;
import com.multideporte.backend.publicportal.dto.PublicTeamSummaryResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentCalendarResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentDetailResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentResultsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentStandingsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentSummaryResponse;
import com.multideporte.backend.publicportal.service.PublicPortalService;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.repository.SportRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.repository.TournamentSpecifications;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPortalServiceImpl implements PublicPortalService {

    private static final Set<TournamentStatus> PUBLIC_TOURNAMENT_STATUSES = EnumSet.of(
            TournamentStatus.OPEN,
            TournamentStatus.IN_PROGRESS,
            TournamentStatus.FINISHED
    );

    private static final PublicReadModulesResponse PUBLIC_MODULES = new PublicReadModulesResponse(true, true, true, false);

    private final TournamentRepository tournamentRepository;
    private final SportRepository sportRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final StandingRepository standingRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final CompetitionAdvancedService competitionAdvancedService;

    @Override
    public PublicPortalHomeResponse getHome() {
        List<Tournament> visibleTournaments = tournamentRepository.findAll(publicVisibilitySpecification(null, null, null));

        long liveCount = visibleTournaments.stream().filter(tournament -> tournament.getStatus() == TournamentStatus.IN_PROGRESS).count();
        long upcomingCount = visibleTournaments.stream().filter(tournament -> tournament.getStatus() == TournamentStatus.OPEN).count();
        long completedCount = visibleTournaments.stream().filter(tournament -> tournament.getStatus() == TournamentStatus.FINISHED).count();

        List<PublicTournamentSummaryResponse> featured = visibleTournaments.stream()
                .sorted(Comparator
                        .comparing(this::publicStatusPriority)
                        .thenComparing(Tournament::getStartDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Tournament::getId, Comparator.reverseOrder()))
                .limit(6)
                .map(this::toSummaryResponse)
                .toList();

        return new PublicPortalHomeResponse(
                "Sistema Campeonatos",
                OffsetDateTime.now(),
                visibleTournaments.size(),
                liveCount,
                upcomingCount,
                completedCount,
                featured,
                PUBLIC_MODULES
        );
    }

    @Override
    public Page<PublicTournamentSummaryResponse> getTournaments(String name, Long sportId, TournamentStatus status, Pageable pageable) {
        return tournamentRepository.findAll(publicVisibilitySpecification(name, sportId, status), pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    public PublicTournamentDetailResponse getTournamentDetail(String slug) {
        Tournament tournament = findVisibleTournamentBySlug(slug);
        Sport sport = loadSport(tournament.getSportId());

        return new PublicTournamentDetailResponse(
                tournament.getId(),
                tournament.getSportId(),
                sport != null ? sport.getName() : null,
                tournament.getName(),
                tournament.getSlug(),
                tournament.getSeasonName(),
                tournament.getFormat(),
                tournament.getStatus(),
                tournament.getDescription(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getUpdatedAt(),
                PUBLIC_MODULES
        );
    }

    @Override
    public PublicTournamentStandingsResponse getTournamentStandings(String slug, Long stageId, Long groupId) {
        Tournament tournament = findVisibleTournamentBySlug(slug);
        validateStageAndGroup(tournament.getId(), stageId, groupId);

        List<Standing> standings = loadStandings(tournament.getId(), stageId, groupId);
        Map<Long, PublicTeamSummaryResponse> teamsByTournamentTeamId = loadTeamsByTournamentTeamId(standings.stream()
                .map(Standing::getTournamentTeamId)
                .collect(Collectors.toSet()));
        TournamentStage stage = stageId != null ? tournamentStageRepository.findById(stageId).orElse(null) : null;
        StageGroup group = groupId != null ? stageGroupRepository.findById(groupId).orElse(null) : null;

        List<PublicStandingEntryResponse> entries = standings.stream()
                .sorted(Comparator
                        .comparing(Standing::getRankPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Standing::getId))
                .map(standing -> new PublicStandingEntryResponse(
                        standing.getId(),
                        standing.getRankPosition(),
                        teamsByTournamentTeamId.get(standing.getTournamentTeamId()),
                        standing.getPlayed(),
                        standing.getWins(),
                        standing.getDraws(),
                        standing.getLosses(),
                        standing.getPointsFor(),
                        standing.getPointsAgainst(),
                        standing.getScoreDiff(),
                        standing.getPoints(),
                        standing.getUpdatedAt()
                ))
                .toList();

        return new PublicTournamentStandingsResponse(
                tournament.getId(),
                tournament.getSlug(),
                stageId,
                stage != null ? stage.getName() : null,
                stage != null ? stage.getStageType().name() : null,
                groupId,
                group != null ? group.getCode() : null,
                group != null ? group.getName() : null,
                entries.size(),
                entries
        );
    }

    @Override
    public PublicTournamentCalendarResponse getTournamentCalendar(
            String slug,
            Long stageId,
            Long groupId,
            MatchGameStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Tournament tournament = findVisibleTournamentBySlug(slug);
        CompetitionAdvancedCalendarResponse calendar = competitionAdvancedService.getCalendar(
                tournament.getId(),
                stageId,
                groupId,
                status,
                from,
                to
        );

        return new PublicTournamentCalendarResponse(
                calendar.tournamentId(),
                tournament.getSlug(),
                calendar.stageId(),
                calendar.groupId(),
                calendar.status(),
                calendar.from(),
                calendar.to(),
                calendar.totalMatches(),
                calendar.scheduledMatches(),
                calendar.closedMatches(),
                calendar.matches().stream()
                        .map(this::toPublicMatchSummary)
                        .toList()
        );
    }

    @Override
    public PublicTournamentResultsResponse getTournamentResults(String slug, Long stageId, Long groupId) {
        Tournament tournament = findVisibleTournamentBySlug(slug);
        CompetitionAdvancedResultsResponse results = competitionAdvancedService.getResults(tournament.getId(), stageId, groupId);

        return new PublicTournamentResultsResponse(
                results.tournamentId(),
                tournament.getSlug(),
                results.stageId(),
                results.groupId(),
                results.totalClosedMatches(),
                results.results().stream()
                        .map(result -> new PublicTournamentResultsResponse.PublicResultEntryResponse(
                                toPublicMatchSummary(result.match()),
                                result.affectsStandings(),
                                result.standingScope(),
                                result.standingStatus()
                        ))
                        .toList()
        );
    }

    private Specification<Tournament> publicVisibilitySpecification(String name, Long sportId, TournamentStatus status) {
        return TournamentSpecifications.byFilters(name, sportId, status, TournamentOperationalCategory.PRODUCTION, true)
                .and((root, query, builder) -> root.get("status").in(PUBLIC_TOURNAMENT_STATUSES));
    }

    private Tournament findVisibleTournamentBySlug(String slug) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo publico no encontrado con slug: " + slug));

        if (!isPubliclyVisible(tournament)) {
            throw new ResourceNotFoundException("Torneo publico no encontrado con slug: " + slug);
        }
        return tournament;
    }

    private boolean isPubliclyVisible(Tournament tournament) {
        return tournament.getOperationalCategory() == TournamentOperationalCategory.PRODUCTION
                && PUBLIC_TOURNAMENT_STATUSES.contains(tournament.getStatus());
    }

    private Sport loadSport(Long sportId) {
        return sportId == null ? null : sportRepository.findById(sportId).orElse(null);
    }

    private PublicTournamentSummaryResponse toSummaryResponse(Tournament tournament) {
        Sport sport = loadSport(tournament.getSportId());
        return new PublicTournamentSummaryResponse(
                tournament.getId(),
                tournament.getSportId(),
                sport != null ? sport.getName() : null,
                tournament.getName(),
                tournament.getSlug(),
                tournament.getSeasonName(),
                tournament.getFormat(),
                tournament.getStatus(),
                tournament.getDescription(),
                tournament.getStartDate(),
                tournament.getEndDate()
        );
    }

    private int publicStatusPriority(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            return 0;
        }
        if (tournament.getStatus() == TournamentStatus.OPEN) {
            return 1;
        }
        return 2;
    }

    private void validateStageAndGroup(Long tournamentId, Long stageId, Long groupId) {
        if (stageId == null && groupId != null) {
            throw new BusinessException("groupId requiere stageId dentro del contrato publico minimo");
        }

        if (stageId != null) {
            TournamentStage stage = tournamentStageRepository.findById(stageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Stage no encontrado con id: " + stageId));
            if (!Objects.equals(stage.getTournamentId(), tournamentId)) {
                throw new BusinessException("El stage indicado no pertenece al torneo publico solicitado");
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

    private List<Standing> loadStandings(Long tournamentId, Long stageId, Long groupId) {
        if (groupId != null) {
            return standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(tournamentId, stageId, groupId);
        }
        if (stageId != null) {
            return standingRepository.findAllByTournamentIdAndStageIdAndGroupId(tournamentId, stageId, null).stream()
                    .sorted(Comparator
                            .comparing(Standing::getRankPosition, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(Standing::getId))
                    .toList();
        }
        return standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(tournamentId).stream()
                .sorted(Comparator
                        .comparing(Standing::getRankPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Standing::getId))
                .toList();
    }

    private Map<Long, PublicTeamSummaryResponse> loadTeamsByTournamentTeamId(Set<Long> tournamentTeamIds) {
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

        return tournamentTeams.values().stream()
                .collect(Collectors.toMap(
                        TournamentTeam::getId,
                        tournamentTeam -> {
                            Team team = teams.get(tournamentTeam.getTeamId());
                            return new PublicTeamSummaryResponse(
                                    tournamentTeam.getId(),
                                    tournamentTeam.getTeamId(),
                                    team != null ? team.getName() : null,
                                    team != null ? team.getShortName() : null,
                                    team != null ? team.getCode() : null,
                                    tournamentTeam.getSeedNumber()
                            );
                        }
                ));
    }

    private PublicMatchSummaryResponse toPublicMatchSummary(CompetitionAdvancedMatchSummary match) {
        return new PublicMatchSummaryResponse(
                match.matchId(),
                match.stageId(),
                match.stageName(),
                match.stageType(),
                match.groupId(),
                match.groupCode(),
                match.groupName(),
                match.roundNumber(),
                match.matchdayNumber(),
                match.scheduledAt(),
                match.venueName(),
                match.status(),
                match.homeScore(),
                match.awayScore(),
                toPublicTeamSummary(match.homeTeam()),
                toPublicTeamSummary(match.awayTeam()),
                toPublicTeamSummary(match.winnerTeam())
        );
    }

    private PublicTeamSummaryResponse toPublicTeamSummary(CompetitionAdvancedTeamSummary team) {
        if (team == null) {
            return null;
        }
        return new PublicTeamSummaryResponse(
                team.tournamentTeamId(),
                team.teamId(),
                team.teamName(),
                team.shortName(),
                team.code(),
                team.seedNumber()
        );
    }
}
