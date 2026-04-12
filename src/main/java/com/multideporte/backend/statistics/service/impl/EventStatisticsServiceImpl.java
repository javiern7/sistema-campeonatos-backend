package com.multideporte.backend.statistics.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import com.multideporte.backend.matchevent.repository.MatchEventRepository;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.statistics.dto.response.EventStatisticsFiltersResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsMatchResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsPlayerResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsSummaryResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsTeamResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsTraceabilityResponse;
import com.multideporte.backend.statistics.service.EventStatisticsService;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventStatisticsServiceImpl implements EventStatisticsService {

    private static final List<String> INCLUDED_EVENT_TYPES = List.of("SCORE", "YELLOW_CARD", "RED_CARD");
    private static final List<String> EXCLUDED_STATUSES = List.of("ANNULLED");

    private final TournamentRepository tournamentRepository;
    private final MatchGameRepository matchGameRepository;
    private final MatchEventRepository matchEventRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    @Override
    public EventStatisticsResponse getEventStatistics(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId
    ) {
        validateTournament(tournamentId);
        MatchGame filteredMatch = validateMatch(tournamentId, matchId);
        TournamentTeam filteredTournamentTeam = resolveAndValidateTeam(tournamentId, tournamentTeamId, teamId);
        validatePlayer(playerId);

        Long resolvedTournamentTeamId = filteredTournamentTeam != null ? filteredTournamentTeam.getId() : null;
        if (filteredMatch != null && resolvedTournamentTeamId != null
                && !Objects.equals(filteredMatch.getHomeTournamentTeamId(), resolvedTournamentTeamId)
                && !Objects.equals(filteredMatch.getAwayTournamentTeamId(), resolvedTournamentTeamId)) {
            throw new BusinessException("El equipo indicado no participa en el partido solicitado");
        }

        List<MatchEvent> events = matchEventRepository.findActiveDerivedStatisticsEvents(
                tournamentId,
                MatchEventStatus.ACTIVE,
                matchId,
                resolvedTournamentTeamId,
                playerId
        );
        Enrichment enrichment = buildEnrichment(events, filteredMatch, filteredTournamentTeam);
        EventCounter summary = count(events);

        return new EventStatisticsResponse(
                tournamentId,
                new EventStatisticsFiltersResponse(
                        matchId,
                        resolvedTournamentTeamId,
                        filteredTournamentTeam != null ? filteredTournamentTeam.getTeamId() : teamId,
                        playerId
                ),
                new EventStatisticsSummaryResponse(summary.goals(), summary.yellowCards(), summary.redCards(), events.size()),
                buildPlayerStatistics(events, enrichment),
                buildTeamStatistics(events, enrichment),
                buildMatchStatistics(events, enrichment),
                buildTraceability()
        );
    }

    private void validateTournament(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new ResourceNotFoundException("Torneo no encontrado con id: " + tournamentId);
        }
    }

    private MatchGame validateMatch(Long tournamentId, Long matchId) {
        if (matchId == null) {
            return null;
        }
        MatchGame match = matchGameRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partido no encontrado con id: " + matchId));
        if (!Objects.equals(match.getTournamentId(), tournamentId)) {
            throw new BusinessException("El partido indicado no pertenece al torneo solicitado");
        }
        return match;
    }

    private TournamentTeam resolveAndValidateTeam(Long tournamentId, Long tournamentTeamId, Long teamId) {
        if (tournamentTeamId == null && teamId == null) {
            return null;
        }

        TournamentTeam tournamentTeam;
        if (tournamentTeamId != null) {
            tournamentTeam = tournamentTeamRepository.findById(tournamentTeamId)
                    .orElseThrow(() -> new ResourceNotFoundException("TournamentTeam no encontrado con id: " + tournamentTeamId));
            if (!Objects.equals(tournamentTeam.getTournamentId(), tournamentId)) {
                throw new BusinessException("El tournamentTeamId indicado no pertenece al torneo solicitado");
            }
            if (teamId != null && !Objects.equals(tournamentTeam.getTeamId(), teamId)) {
                throw new BusinessException("teamId y tournamentTeamId no corresponden al mismo equipo inscrito");
            }
            return tournamentTeam;
        }

        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Equipo no encontrado con id: " + teamId);
        }
        return tournamentTeamRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .orElseThrow(() -> new BusinessException("El equipo indicado no esta inscrito en el torneo solicitado"));
    }

    private void validatePlayer(Long playerId) {
        if (playerId != null && !playerRepository.existsById(playerId)) {
            throw new ResourceNotFoundException("Jugador no encontrado con id: " + playerId);
        }
    }

    private List<EventStatisticsPlayerResponse> buildPlayerStatistics(List<MatchEvent> events, Enrichment enrichment) {
        return events.stream()
                .filter(event -> event.getPlayerId() != null)
                .collect(Collectors.groupingBy(
                        event -> new PlayerTeamKey(event.getPlayerId(), event.getTournamentTeamId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    PlayerTeamKey key = entry.getKey();
                    EventCounter counter = count(entry.getValue());
                    Player player = enrichment.playersById().get(key.playerId());
                    TournamentTeam tournamentTeam = enrichment.tournamentTeamsById().get(key.tournamentTeamId());
                    Team team = tournamentTeam != null ? enrichment.teamsById().get(tournamentTeam.getTeamId()) : null;
                    return new EventStatisticsPlayerResponse(
                            key.playerId(),
                            player != null ? player.getFirstName() : null,
                            player != null ? player.getLastName() : null,
                            player != null ? displayName(player) : null,
                            key.tournamentTeamId(),
                            tournamentTeam != null ? tournamentTeam.getTeamId() : null,
                            team != null ? team.getName() : null,
                            team != null ? team.getShortName() : null,
                            counter.goals(),
                            counter.yellowCards(),
                            counter.redCards(),
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(EventStatisticsPlayerResponse::goals).reversed()
                        .thenComparing(EventStatisticsPlayerResponse::yellowCards, Comparator.reverseOrder())
                        .thenComparing(EventStatisticsPlayerResponse::redCards, Comparator.reverseOrder())
                        .thenComparing(EventStatisticsPlayerResponse::displayName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EventStatisticsPlayerResponse::playerId))
                .toList();
    }

    private List<EventStatisticsTeamResponse> buildTeamStatistics(List<MatchEvent> events, Enrichment enrichment) {
        return events.stream()
                .filter(event -> event.getTournamentTeamId() != null)
                .collect(Collectors.groupingBy(MatchEvent::getTournamentTeamId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    TournamentTeam tournamentTeam = enrichment.tournamentTeamsById().get(entry.getKey());
                    Team team = tournamentTeam != null ? enrichment.teamsById().get(tournamentTeam.getTeamId()) : null;
                    EventCounter counter = count(entry.getValue());
                    return new EventStatisticsTeamResponse(
                            entry.getKey(),
                            tournamentTeam != null ? tournamentTeam.getTeamId() : null,
                            team != null ? team.getName() : null,
                            team != null ? team.getShortName() : null,
                            team != null ? team.getCode() : null,
                            tournamentTeam != null ? tournamentTeam.getSeedNumber() : null,
                            counter.goals(),
                            counter.yellowCards(),
                            counter.redCards(),
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(EventStatisticsTeamResponse::goals).reversed()
                        .thenComparing(EventStatisticsTeamResponse::yellowCards, Comparator.reverseOrder())
                        .thenComparing(EventStatisticsTeamResponse::redCards, Comparator.reverseOrder())
                        .thenComparing(EventStatisticsTeamResponse::teamName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(EventStatisticsTeamResponse::tournamentTeamId))
                .toList();
    }

    private List<EventStatisticsMatchResponse> buildMatchStatistics(List<MatchEvent> events, Enrichment enrichment) {
        return events.stream()
                .collect(Collectors.groupingBy(MatchEvent::getMatchId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    MatchGame match = enrichment.matchesById().get(entry.getKey());
                    EventCounter counter = count(entry.getValue());
                    return new EventStatisticsMatchResponse(
                            entry.getKey(),
                            match != null ? match.getHomeTournamentTeamId() : null,
                            match != null ? match.getAwayTournamentTeamId() : null,
                            match != null ? match.getScheduledAt() : null,
                            counter.goals(),
                            counter.yellowCards(),
                            counter.redCards(),
                            entry.getValue().size()
                    );
                })
                .sorted(Comparator.comparing(EventStatisticsMatchResponse::scheduledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EventStatisticsMatchResponse::matchId))
                .toList();
    }

    private Enrichment buildEnrichment(List<MatchEvent> events, MatchGame filteredMatch, TournamentTeam filteredTournamentTeam) {
        Set<Long> matchIds = events.stream().map(MatchEvent::getMatchId).collect(Collectors.toSet());
        if (filteredMatch != null) {
            matchIds.add(filteredMatch.getId());
        }

        Set<Long> tournamentTeamIds = events.stream()
                .map(MatchEvent::getTournamentTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (filteredTournamentTeam != null) {
            tournamentTeamIds.add(filteredTournamentTeam.getId());
        }

        Set<Long> playerIds = events.stream()
                .map(MatchEvent::getPlayerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, MatchGame> matchesById = matchGameRepository.findAllById(matchIds).stream()
                .collect(Collectors.toMap(MatchGame::getId, Function.identity()));
        Map<Long, TournamentTeam> tournamentTeamsById = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Set<Long> teamIds = tournamentTeamsById.values().stream()
                .map(TournamentTeam::getTeamId)
                .collect(Collectors.toSet());
        Map<Long, Team> teamsById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, Player> playersById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        return new Enrichment(matchesById, tournamentTeamsById, teamsById, playersById);
    }

    private EventCounter count(List<MatchEvent> events) {
        int goals = 0;
        int yellowCards = 0;
        int redCards = 0;

        for (MatchEvent event : events) {
            if (event.getEventType() == MatchEventType.SCORE) {
                goals += event.getEventValue() != null ? event.getEventValue() : 1;
            } else if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                yellowCards++;
            } else if (event.getEventType() == MatchEventType.RED_CARD) {
                redCards++;
            }
        }

        return new EventCounter(goals, yellowCards, redCards);
    }

    private EventStatisticsTraceabilityResponse buildTraceability() {
        return new EventStatisticsTraceabilityResponse(
                true,
                "match_event",
                INCLUDED_EVENT_TYPES,
                EXCLUDED_STATUSES,
                List.of(
                        "Solo se agregan eventos con status ACTIVE",
                        "SCORE suma eventValue; YELLOW_CARD y RED_CARD suman una unidad por evento",
                        "No recalcula standings ni modifica resultados oficiales"
                )
        );
    }

    private String displayName(Player player) {
        return (player.getFirstName() + " " + player.getLastName()).trim();
    }

    private record PlayerTeamKey(Long playerId, Long tournamentTeamId) {
    }

    private record EventCounter(Integer goals, Integer yellowCards, Integer redCards) {
    }

    private record Enrichment(
            Map<Long, MatchGame> matchesById,
            Map<Long, TournamentTeam> tournamentTeamsById,
            Map<Long, Team> teamsById,
            Map<Long, Player> playersById
    ) {
    }
}
