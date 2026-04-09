package com.multideporte.backend.discipline.service.impl;

import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.discipline.dto.request.DisciplinaryIncidentCreateRequest;
import com.multideporte.backend.discipline.dto.request.DisciplinarySanctionCreateRequest;
import com.multideporte.backend.discipline.dto.response.DisciplineMatchResponse;
import com.multideporte.backend.discipline.dto.response.DisciplineMatchSummaryResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinePlayerResponse;
import com.multideporte.backend.discipline.dto.response.DisciplineTeamResponse;
import com.multideporte.backend.discipline.dto.response.DisciplineTraceabilityResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinaryIncidentResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionListResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionResponse;
import com.multideporte.backend.discipline.entity.DisciplinaryIncident;
import com.multideporte.backend.discipline.entity.DisciplinarySanction;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionStatus;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import com.multideporte.backend.discipline.repository.DisciplinaryIncidentRepository;
import com.multideporte.backend.discipline.repository.DisciplinarySanctionRepository;
import com.multideporte.backend.discipline.service.DisciplineService;
import com.multideporte.backend.discipline.validation.DisciplineValidator;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
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
public class DisciplineServiceImpl implements DisciplineService {

    private static final Set<MatchGameStatus> CLOSED_MATCH_STATUSES = Set.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT);

    private final DisciplineValidator disciplineValidator;
    private final DisciplinaryIncidentRepository disciplinaryIncidentRepository;
    private final DisciplinarySanctionRepository disciplinarySanctionRepository;
    private final MatchGameRepository matchGameRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final CurrentUserService currentUserService;

    @Override
    public DisciplineMatchResponse getMatchDiscipline(Long matchId) {
        MatchGame match = disciplineValidator.requireValidMatch(matchId);
        List<DisciplinaryIncident> incidents = disciplinaryIncidentRepository.findAllByMatchIdOrderByCreatedAtAscIdAsc(matchId);
        List<DisciplinarySanction> sanctions = loadSanctionsForIncidents(incidents);
        EnrichmentContext context = buildContext(match, incidents, sanctions);

        return new DisciplineMatchResponse(
                toMatchSummary(match, context.teamsByTournamentTeamId()),
                incidents.stream()
                        .map(incident -> toIncidentResponse(
                                incident,
                                context.teamsByTournamentTeamId(),
                                context.playersById(),
                                context.sanctionExistsByIncidentId().containsKey(incident.getId())
                        ))
                        .toList(),
                sanctions.stream()
                        .map(sanction -> toSanctionResponse(
                                sanction,
                                context.incidentsById().get(sanction.getIncidentId()),
                                context.teamsByTournamentTeamId(),
                                context.playersById()
                        ))
                        .toList(),
                new DisciplineTraceabilityResponse(
                        "MATCH_GAME",
                        "TOURNAMENT_TEAM_ROSTER_BY_MATCH_DATE",
                        "DERIVED_FROM_SANCTION_AND_SUBSEQUENT_CLOSED_MATCHES"
                )
        );
    }

    @Override
    @Transactional
    public DisciplinaryIncidentResponse createIncident(Long matchId, DisciplinaryIncidentCreateRequest request) {
        MatchGame match = disciplineValidator.requireValidMatch(matchId);
        disciplineValidator.validateIncidentCreate(match, request.tournamentTeamId(), request.playerId());

        DisciplinaryIncident incident = new DisciplinaryIncident();
        incident.setTournamentId(match.getTournamentId());
        incident.setMatchId(match.getId());
        incident.setTournamentTeamId(request.tournamentTeamId());
        incident.setPlayerId(request.playerId());
        incident.setIncidentType(request.incidentType());
        incident.setIncidentMinute(request.incidentMinute());
        incident.setNotes(request.notes());
        incident.setCreatedByUserId(currentUserService.requireCurrentUserId());

        DisciplinaryIncident saved = disciplinaryIncidentRepository.save(incident);
        EnrichmentContext context = buildContext(match, List.of(saved), List.of());
        return toIncidentResponse(saved, context.teamsByTournamentTeamId(), context.playersById(), false);
    }

    @Override
    @Transactional
    public DisciplinarySanctionResponse createSanction(Long matchId, Long incidentId, DisciplinarySanctionCreateRequest request) {
        MatchGame match = disciplineValidator.requireValidMatch(matchId);
        DisciplinaryIncident incident = disciplinaryIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia disciplinaria no encontrada con id: " + incidentId));
        disciplineValidator.validateSanctionCreate(match, incident, request.sanctionType(), request.matchesToServe());

        int normalizedMatchesToServe = request.matchesToServe() == null ? 0 : request.matchesToServe();
        DisciplinarySanction sanction = new DisciplinarySanction();
        sanction.setTournamentId(match.getTournamentId());
        sanction.setIncidentId(incident.getId());
        sanction.setPlayerId(incident.getPlayerId());
        sanction.setTournamentTeamId(incident.getTournamentTeamId());
        sanction.setSanctionType(request.sanctionType());
        sanction.setMatchesToServe(normalizedMatchesToServe);
        sanction.setStatus(normalizedMatchesToServe == 0 ? DisciplinarySanctionStatus.SERVED : DisciplinarySanctionStatus.ACTIVE);
        sanction.setNotes(request.notes());
        sanction.setCreatedByUserId(currentUserService.requireCurrentUserId());

        DisciplinarySanction saved = disciplinarySanctionRepository.save(sanction);
        EnrichmentContext context = buildContext(match, List.of(incident), List.of(saved));
        return toSanctionResponse(saved, incident, context.teamsByTournamentTeamId(), context.playersById());
    }

    @Override
    public DisciplinarySanctionListResponse getTournamentSanctions(
            Long tournamentId,
            DisciplinarySanctionStatus status,
            Long teamId,
            Long playerId,
            Long matchId,
            Boolean activeOnly
    ) {
        List<DisciplinarySanction> sanctions = disciplinarySanctionRepository.findAllByTournamentIdOrderByCreatedAtDescIdDesc(tournamentId);
        if (sanctions.isEmpty()) {
            return new DisciplinarySanctionListResponse(tournamentId, 0, List.of());
        }

        List<DisciplinaryIncident> incidents = disciplinaryIncidentRepository.findAllByIdIn(
                sanctions.stream().map(DisciplinarySanction::getIncidentId).toList()
        );
        MatchGame baseMatch = matchGameRepository.findById(incidents.get(0).getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("MatchGame no encontrado para contexto disciplinario"));
        EnrichmentContext context = buildContext(baseMatch, incidents, sanctions);

        List<DisciplinarySanctionResponse> responses = sanctions.stream()
                .map(sanction -> toSanctionResponse(
                        sanction,
                        context.incidentsById().get(sanction.getIncidentId()),
                        context.teamsByTournamentTeamId(),
                        context.playersById()
                ))
                .filter(response -> status == null || response.status() == status)
                .filter(response -> teamId == null || Objects.equals(response.team().teamId(), teamId))
                .filter(response -> playerId == null || Objects.equals(response.player().playerId(), playerId))
                .filter(response -> matchId == null || Objects.equals(response.matchId(), matchId))
                .filter(response -> !Boolean.TRUE.equals(activeOnly) || response.status() == DisciplinarySanctionStatus.ACTIVE)
                .toList();

        return new DisciplinarySanctionListResponse(tournamentId, responses.size(), responses);
    }

    private List<DisciplinarySanction> loadSanctionsForIncidents(List<DisciplinaryIncident> incidents) {
        if (incidents.isEmpty()) {
            return List.of();
        }
        return disciplinarySanctionRepository.findAllByIncidentIdInOrderByCreatedAtAscIdAsc(
                incidents.stream().map(DisciplinaryIncident::getId).toList()
        );
    }

    private EnrichmentContext buildContext(
            MatchGame baseMatch,
            List<DisciplinaryIncident> incidents,
            List<DisciplinarySanction> sanctions
    ) {
        Map<Long, DisciplinaryIncident> incidentsById = incidents.stream()
                .collect(Collectors.toMap(DisciplinaryIncident::getId, Function.identity(), (left, right) -> left));
        Map<Long, Boolean> sanctionExistsByIncidentId = new HashMap<>();
        for (DisciplinarySanction sanction : sanctions) {
            sanctionExistsByIncidentId.put(sanction.getIncidentId(), true);
        }

        Set<Long> tournamentTeamIds = incidents.stream()
                .map(DisciplinaryIncident::getTournamentTeamId)
                .collect(Collectors.toSet());
        tournamentTeamIds.add(baseMatch.getHomeTournamentTeamId());
        tournamentTeamIds.add(baseMatch.getAwayTournamentTeamId());

        Map<Long, TournamentTeam> tournamentTeamsById = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Set<Long> teamIds = tournamentTeamsById.values().stream()
                .map(TournamentTeam::getTeamId)
                .collect(Collectors.toSet());
        Map<Long, Team> teamsById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        Map<Long, DisciplineTeamResponse> teamsByTournamentTeamId = new HashMap<>();
        for (Map.Entry<Long, TournamentTeam> entry : tournamentTeamsById.entrySet()) {
            Team team = teamsById.get(entry.getValue().getTeamId());
            teamsByTournamentTeamId.put(entry.getKey(), new DisciplineTeamResponse(
                    entry.getKey(),
                    entry.getValue().getTeamId(),
                    team != null ? team.getName() : null,
                    team != null ? team.getShortName() : null,
                    team != null ? team.getCode() : null
            ));
        }

        Set<Long> playerIds = incidents.stream().map(DisciplinaryIncident::getPlayerId).collect(Collectors.toSet());
        sanctions.stream().map(DisciplinarySanction::getPlayerId).forEach(playerIds::add);
        Map<Long, DisciplinePlayerResponse> playersById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, this::toPlayerResponse));

        return new EnrichmentContext(incidentsById, sanctionExistsByIncidentId, teamsByTournamentTeamId, playersById);
    }

    private DisciplineMatchSummaryResponse toMatchSummary(
            MatchGame match,
            Map<Long, DisciplineTeamResponse> teamsByTournamentTeamId
    ) {
        return new DisciplineMatchSummaryResponse(
                match.getId(),
                match.getTournamentId(),
                match.getStageId(),
                match.getGroupId(),
                match.getScheduledAt(),
                match.getStatus(),
                teamsByTournamentTeamId.get(match.getHomeTournamentTeamId()),
                teamsByTournamentTeamId.get(match.getAwayTournamentTeamId())
        );
    }

    private DisciplinaryIncidentResponse toIncidentResponse(
            DisciplinaryIncident incident,
            Map<Long, DisciplineTeamResponse> teamsByTournamentTeamId,
            Map<Long, DisciplinePlayerResponse> playersById,
            boolean sanctionRegistered
    ) {
        return new DisciplinaryIncidentResponse(
                incident.getId(),
                incident.getMatchId(),
                incident.getTournamentId(),
                teamsByTournamentTeamId.get(incident.getTournamentTeamId()),
                playersById.get(incident.getPlayerId()),
                incident.getIncidentType(),
                incident.getIncidentMinute(),
                incident.getNotes(),
                incident.getCreatedAt(),
                sanctionRegistered
        );
    }

    private DisciplinarySanctionResponse toSanctionResponse(
            DisciplinarySanction sanction,
            DisciplinaryIncident incident,
            Map<Long, DisciplineTeamResponse> teamsByTournamentTeamId,
            Map<Long, DisciplinePlayerResponse> playersById
    ) {
        int matchesServed = deriveMatchesServed(sanction, incident);
        int remainingMatches = Math.max(0, sanction.getMatchesToServe() - matchesServed);
        DisciplinarySanctionStatus derivedStatus = remainingMatches == 0
                ? DisciplinarySanctionStatus.SERVED
                : DisciplinarySanctionStatus.ACTIVE;

        return new DisciplinarySanctionResponse(
                sanction.getId(),
                sanction.getIncidentId(),
                incident.getMatchId(),
                sanction.getTournamentId(),
                teamsByTournamentTeamId.get(sanction.getTournamentTeamId()),
                playersById.get(sanction.getPlayerId()),
                sanction.getSanctionType(),
                derivedStatus,
                sanction.getMatchesToServe(),
                matchesServed,
                remainingMatches,
                sanction.getCreatedAt(),
                sanction.getNotes()
        );
    }

    private int deriveMatchesServed(DisciplinarySanction sanction, DisciplinaryIncident incident) {
        if (sanction.getSanctionType() != DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO || sanction.getMatchesToServe() == 0) {
            return 0;
        }

        MatchGame originMatch = matchGameRepository.findById(incident.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("MatchGame no encontrado para la sancion disciplinaria"));

        List<MatchGame> teamMatches = matchGameRepository.findAll().stream()
                .filter(match -> Objects.equals(match.getTournamentId(), sanction.getTournamentId()))
                .filter(match -> CLOSED_MATCH_STATUSES.contains(match.getStatus()))
                .filter(match -> Objects.equals(match.getHomeTournamentTeamId(), sanction.getTournamentTeamId())
                        || Objects.equals(match.getAwayTournamentTeamId(), sanction.getTournamentTeamId()))
                .filter(match -> isSubsequentMatch(originMatch, match))
                .sorted(Comparator
                        .comparing(MatchGame::getScheduledAt, Comparator.nullsLast(OffsetDateTime::compareTo))
                        .thenComparing(MatchGame::getId))
                .toList();

        return Math.min(teamMatches.size(), sanction.getMatchesToServe());
    }

    private boolean isSubsequentMatch(MatchGame originMatch, MatchGame candidate) {
        if (Objects.equals(originMatch.getId(), candidate.getId())) {
            return false;
        }

        if (originMatch.getScheduledAt() != null && candidate.getScheduledAt() != null) {
            if (candidate.getScheduledAt().isAfter(originMatch.getScheduledAt())) {
                return true;
            }
            if (candidate.getScheduledAt().isEqual(originMatch.getScheduledAt())) {
                return candidate.getId() > originMatch.getId();
            }
            return false;
        }

        return candidate.getId() > originMatch.getId();
    }

    private DisciplinePlayerResponse toPlayerResponse(Player player) {
        return new DisciplinePlayerResponse(
                player.getId(),
                (player.getFirstName() + " " + player.getLastName()).trim(),
                player.getActive()
        );
    }

    private record EnrichmentContext(
            Map<Long, DisciplinaryIncident> incidentsById,
            Map<Long, Boolean> sanctionExistsByIncidentId,
            Map<Long, DisciplineTeamResponse> teamsByTournamentTeamId,
            Map<Long, DisciplinePlayerResponse> playersById
    ) {
    }
}
