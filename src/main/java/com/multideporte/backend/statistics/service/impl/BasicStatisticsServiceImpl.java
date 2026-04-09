package com.multideporte.backend.statistics.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
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
import com.multideporte.backend.statistics.dto.response.BasicStatisticsLeaderResponse;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsLeadersResponse;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsResponse;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsSummaryResponse;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsTeamResponse;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsTraceabilityResponse;
import com.multideporte.backend.statistics.service.BasicStatisticsService;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class BasicStatisticsServiceImpl implements BasicStatisticsService {

    private static final Set<MatchGameStatus> CLOSED_MATCH_STATUSES = Set.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT);
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_PENDING_RECALCULATION = "PENDING_RECALCULATION";
    private static final String STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;

    @Override
    public BasicStatisticsResponse getBasicStatistics(Long tournamentId, Long stageId, Long groupId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado con id: " + tournamentId));
        TournamentStage stage = validateStageAndGroup(tournamentId, stageId, groupId);

        List<MatchGame> matches = fetchMatches(tournamentId, stageId, groupId);
        BasicStatisticsSummaryResponse summary = buildSummary(tournamentId, matches);
        LeaderBundle leaderBundle = buildLeaders(tournamentId, stage, groupId);

        return new BasicStatisticsResponse(
                tournament.getId(),
                stageId,
                groupId,
                summary,
                leaderBundle.leaders(),
                buildTraceability(leaderBundle, stage, groupId)
        );
    }

    private TournamentStage validateStageAndGroup(Long tournamentId, Long stageId, Long groupId) {
        if (stageId == null && groupId != null) {
            throw new BusinessException("groupId requiere stageId dentro del contrato de estadisticas basicas");
        }

        TournamentStage stage = null;
        if (stageId != null) {
            stage = tournamentStageRepository.findById(stageId)
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

        return stage;
    }

    private List<MatchGame> fetchMatches(Long tournamentId, Long stageId, Long groupId) {
        Specification<MatchGame> specification = MatchGameSpecifications.byFilters(tournamentId, stageId, groupId, null);
        return matchGameRepository.findAll(specification);
    }

    private BasicStatisticsSummaryResponse buildSummary(Long tournamentId, List<MatchGame> matches) {
        int playedMatches = 0;
        int scheduledMatches = 0;
        int forfeitMatches = 0;
        int cancelledMatches = 0;
        int scoredPoints = 0;
        OffsetDateTime lastPlayedAt = null;

        for (MatchGame match : matches) {
            if (match.getStatus() == MatchGameStatus.PLAYED) {
                playedMatches++;
            } else if (match.getStatus() == MatchGameStatus.SCHEDULED) {
                scheduledMatches++;
            } else if (match.getStatus() == MatchGameStatus.FORFEIT) {
                forfeitMatches++;
            } else if (match.getStatus() == MatchGameStatus.CANCELLED) {
                cancelledMatches++;
            }

            if (CLOSED_MATCH_STATUSES.contains(match.getStatus())) {
                scoredPoints += safeScore(match.getHomeScore()) + safeScore(match.getAwayScore());
                if (match.getScheduledAt() != null && (lastPlayedAt == null || match.getScheduledAt().isAfter(lastPlayedAt))) {
                    lastPlayedAt = match.getScheduledAt();
                }
            }
        }

        double average = playedMatches == 0 ? 0D : (double) scoredPoints / playedMatches;

        return new BasicStatisticsSummaryResponse(
                tournamentTeamRepository.countApprovedTeamsWithActiveRosterSupport(tournamentId),
                matches.size(),
                playedMatches,
                scheduledMatches,
                forfeitMatches,
                cancelledMatches,
                scoredPoints,
                scoredPoints,
                average,
                lastPlayedAt
        );
    }

    private LeaderBundle buildLeaders(Long tournamentId, TournamentStage stage, Long groupId) {
        String scope = resolveScope(stage, groupId);
        if (stage != null && stage.getStageType() == TournamentStageType.KNOCKOUT) {
            return new LeaderBundle(
                    new BasicStatisticsLeadersResponse(
                            notApplicableLeader("POINTS", scope),
                            notApplicableLeader("WINS", scope),
                            notApplicableLeader("SCORE_DIFF", scope),
                            notApplicableLeader("POINTS_FOR", scope)
                    ),
                    false,
                    STATUS_NOT_APPLICABLE,
                    List.of("Los lideres de clasificacion no aplican a etapas KNOCKOUT")
            );
        }

        List<Standing> standings = loadStandings(tournamentId, stage, groupId);
        if (standings.isEmpty()) {
            return new LeaderBundle(
                    new BasicStatisticsLeadersResponse(
                            pendingLeader("POINTS", scope),
                            pendingLeader("WINS", scope),
                            pendingLeader("SCORE_DIFF", scope),
                            pendingLeader("POINTS_FOR", scope)
                    ),
                    false,
                    "STANDINGS_PENDING",
                    List.of("No existen standings disponibles para resolver lideres en el scope solicitado")
            );
        }

        Map<Long, BasicStatisticsTeamResponse> teamsByTournamentTeamId = buildTeamsByTournamentTeamId(standings);
        return new LeaderBundle(
                new BasicStatisticsLeadersResponse(
                        resolveLeader("POINTS", scope, standings, teamsByTournamentTeamId, Standing::getPoints),
                        resolveLeader("WINS", scope, standings, teamsByTournamentTeamId, Standing::getWins),
                        resolveLeader("SCORE_DIFF", scope, standings, teamsByTournamentTeamId, Standing::getScoreDiff),
                        resolveLeader("POINTS_FOR", scope, standings, teamsByTournamentTeamId, Standing::getPointsFor)
                ),
                true,
                "STANDINGS",
                List.of()
        );
    }

    private List<Standing> loadStandings(Long tournamentId, TournamentStage stage, Long groupId) {
        List<Standing> standings;
        if (groupId != null) {
            standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(
                    tournamentId,
                    stage.getId(),
                    groupId
            );
        } else if (stage != null) {
            standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupIdIsNull(
                    tournamentId,
                    stage.getId()
            );
        } else {
            standings = standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(tournamentId);
        }

        return standings.stream()
                .sorted(Comparator.comparing(Standing::getRankPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Standing::getTournamentTeamId))
                .toList();
    }

    private Map<Long, BasicStatisticsTeamResponse> buildTeamsByTournamentTeamId(List<Standing> standings) {
        Set<Long> tournamentTeamIds = standings.stream()
                .map(Standing::getTournamentTeamId)
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

        Map<Long, Integer> rankByTournamentTeamId = standings.stream()
                .collect(Collectors.toMap(
                        Standing::getTournamentTeamId,
                        Standing::getRankPosition,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, BasicStatisticsTeamResponse> response = new HashMap<>();
        for (Long tournamentTeamId : tournamentTeamIds) {
            TournamentTeam tournamentTeam = tournamentTeams.get(tournamentTeamId);
            if (tournamentTeam == null) {
                continue;
            }

            Team team = teams.get(tournamentTeam.getTeamId());
            response.put(tournamentTeamId, new BasicStatisticsTeamResponse(
                    tournamentTeamId,
                    tournamentTeam.getTeamId(),
                    team != null ? team.getName() : null,
                    team != null ? team.getShortName() : null,
                    team != null ? team.getCode() : null,
                    tournamentTeam.getSeedNumber(),
                    rankByTournamentTeamId.get(tournamentTeamId)
            ));
        }
        return response;
    }

    private BasicStatisticsLeaderResponse resolveLeader(
            String metric,
            String scope,
            List<Standing> standings,
            Map<Long, BasicStatisticsTeamResponse> teamsByTournamentTeamId,
            Function<Standing, Integer> metricExtractor
    ) {
        Optional<Standing> topStanding = standings.stream()
                .max(Comparator.comparing(metricExtractor).thenComparing(Standing::getTournamentTeamId));

        if (topStanding.isEmpty()) {
            return pendingLeader(metric, scope);
        }

        int topValue = metricExtractor.apply(topStanding.get());
        int tieCount = (int) standings.stream()
                .filter(standing -> metricExtractor.apply(standing) == topValue)
                .count();

        return new BasicStatisticsLeaderResponse(
                metric,
                STATUS_AVAILABLE,
                scope,
                topValue,
                tieCount,
                teamsByTournamentTeamId.get(topStanding.get().getTournamentTeamId())
        );
    }

    private BasicStatisticsLeaderResponse pendingLeader(String metric, String scope) {
        return new BasicStatisticsLeaderResponse(metric, STATUS_PENDING_RECALCULATION, scope, null, 0, null);
    }

    private BasicStatisticsLeaderResponse notApplicableLeader(String metric, String scope) {
        return new BasicStatisticsLeaderResponse(metric, STATUS_NOT_APPLICABLE, scope, null, 0, null);
    }

    private BasicStatisticsTraceabilityResponse buildTraceability(LeaderBundle leaderBundle, TournamentStage stage, Long groupId) {
        List<String> notes = new ArrayList<>(leaderBundle.notes());
        if (groupId != null) {
            notes.add("El scope solicitado corresponde a estadisticas por grupo");
        } else if (stage != null) {
            notes.add("El scope solicitado corresponde a estadisticas por etapa");
        } else {
            notes.add("El scope solicitado corresponde a estadisticas a nivel torneo");
        }

        return new BasicStatisticsTraceabilityResponse(
                true,
                leaderBundle.derivedFromStandings(),
                leaderBundle.classificationSource(),
                notes
        );
    }

    private String resolveScope(TournamentStage stage, Long groupId) {
        if (groupId != null) {
            return "GROUP";
        }
        if (stage != null) {
            return stage.getStageType().name();
        }
        return "TOURNAMENT";
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private record LeaderBundle(
            BasicStatisticsLeadersResponse leaders,
            boolean derivedFromStandings,
            String classificationSource,
            List<String> notes
    ) {
    }
}
