package com.multideporte.backend.discipline.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.discipline.dto.request.DisciplinaryIncidentCreateRequest;
import com.multideporte.backend.discipline.dto.request.DisciplinarySanctionCreateRequest;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionListResponse;
import com.multideporte.backend.discipline.entity.DisciplinaryIncident;
import com.multideporte.backend.discipline.entity.DisciplinaryIncidentType;
import com.multideporte.backend.discipline.entity.DisciplinarySanction;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionStatus;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import com.multideporte.backend.discipline.repository.DisciplinaryIncidentRepository;
import com.multideporte.backend.discipline.repository.DisciplinarySanctionRepository;
import com.multideporte.backend.discipline.service.impl.DisciplineServiceImpl;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisciplineServiceImplTest {

    @Mock
    private DisciplineValidator disciplineValidator;
    @Mock
    private DisciplinaryIncidentRepository disciplinaryIncidentRepository;
    @Mock
    private DisciplinarySanctionRepository disciplinarySanctionRepository;
    @Mock
    private MatchGameRepository matchGameRepository;
    @Mock
    private TournamentTeamRepository tournamentTeamRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DisciplineServiceImpl service;

    @Test
    void shouldCreateIncidentWithTraceableContext() {
        MatchGame match = closedMatch(10L, 1L, 101L, 102L);
        when(disciplineValidator.requireValidMatch(match.getId())).thenReturn(match);
        when(currentUserService.requireCurrentUserId()).thenReturn(99L);

        DisciplinaryIncident saved = new DisciplinaryIncident();
        saved.setId(1000L);
        saved.setMatchId(match.getId());
        saved.setTournamentId(match.getTournamentId());
        saved.setTournamentTeamId(101L);
        saved.setPlayerId(500L);
        saved.setIncidentType(DisciplinaryIncidentType.EXPULSION);
        saved.setIncidentMinute(70);
        saved.setCreatedAt(OffsetDateTime.parse("2026-04-08T18:00:00Z"));
        when(disciplinaryIncidentRepository.save(any(DisciplinaryIncident.class))).thenReturn(saved);
        mockContext(match);

        var response = service.createIncident(match.getId(), new DisciplinaryIncidentCreateRequest(
                101L,
                500L,
                DisciplinaryIncidentType.EXPULSION,
                70,
                "Falta grave"
        ));

        assertEquals(saved.getId(), response.incidentId());
        assertEquals("Jugador Uno", response.player().fullName());
        assertEquals("Halcones", response.team().name());
        assertEquals(DisciplinaryIncidentType.EXPULSION, response.incidentType());
    }

    @Test
    void shouldCreateMatchSuspensionSanction() {
        MatchGame match = closedMatch(10L, 1L, 101L, 102L);
        DisciplinaryIncident incident = new DisciplinaryIncident();
        incident.setId(2000L);
        incident.setMatchId(match.getId());
        incident.setTournamentId(match.getTournamentId());
        incident.setTournamentTeamId(101L);
        incident.setPlayerId(500L);

        when(disciplineValidator.requireValidMatch(match.getId())).thenReturn(match);
        when(disciplinaryIncidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(currentUserService.requireCurrentUserId()).thenReturn(99L);

        DisciplinarySanction saved = new DisciplinarySanction();
        saved.setId(3000L);
        saved.setIncidentId(incident.getId());
        saved.setTournamentId(match.getTournamentId());
        saved.setTournamentTeamId(incident.getTournamentTeamId());
        saved.setPlayerId(incident.getPlayerId());
        saved.setSanctionType(DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO);
        saved.setMatchesToServe(1);
        saved.setStatus(DisciplinarySanctionStatus.ACTIVE);
        saved.setCreatedAt(OffsetDateTime.parse("2026-04-08T18:10:00Z"));
        when(disciplinarySanctionRepository.save(any(DisciplinarySanction.class))).thenReturn(saved);
        when(matchGameRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchGameRepository.findAll()).thenReturn(List.of(match));
        mockContext(match);

        var response = service.createSanction(match.getId(), incident.getId(), new DisciplinarySanctionCreateRequest(
                DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO,
                1,
                "Suspension automatica simple"
        ));

        assertEquals(saved.getId(), response.sanctionId());
        assertEquals(1, response.matchesToServe());
        assertEquals(1, response.remainingMatches());
        assertEquals(DisciplinarySanctionStatus.ACTIVE, response.status());
    }

    @Test
    void shouldFilterTournamentSanctionsByActiveStatus() {
        MatchGame match = closedMatch(10L, 1L, 101L, 102L);
        DisciplinaryIncident incident = new DisciplinaryIncident();
        incident.setId(2000L);
        incident.setMatchId(match.getId());
        incident.setTournamentId(match.getTournamentId());
        incident.setTournamentTeamId(101L);
        incident.setPlayerId(500L);

        DisciplinarySanction annotation = sanction(1L, incident.getId(), incident.getTournamentId(), incident.getTournamentTeamId(),
                incident.getPlayerId(), DisciplinarySanctionType.ANOTACION_DISCIPLINARIA, 0);
        DisciplinarySanction suspension = sanction(2L, incident.getId(), incident.getTournamentId(), incident.getTournamentTeamId(),
                incident.getPlayerId(), DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO, 1);

        when(disciplinarySanctionRepository.findAllByTournamentIdOrderByCreatedAtDescIdDesc(match.getTournamentId()))
                .thenReturn(List.of(annotation, suspension));
        when(disciplinaryIncidentRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(incident));
        when(matchGameRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(matchGameRepository.findAll()).thenReturn(List.of(match));
        mockContext(match);

        DisciplinarySanctionListResponse response = service.getTournamentSanctions(
                match.getTournamentId(),
                null,
                null,
                null,
                null,
                true
        );

        assertEquals(1, response.totalSanctions());
        assertEquals(DisciplinarySanctionType.SUSPENSION_PROXIMO_PARTIDO, response.sanctions().get(0).sanctionType());
    }

    @Test
    void shouldRejectTeamThatDoesNotBelongToMatch() {
        MatchGame match = closedMatch(10L, 1L, 101L, 102L);
        when(disciplineValidator.requireValidMatch(match.getId())).thenReturn(match);
        Mockito.doThrow(new BusinessException("El tournamentTeamId enviado no participa en el partido indicado"))
                .when(disciplineValidator)
                .validateIncidentCreate(match, 999L, 500L);

        assertThrows(BusinessException.class, () -> service.createIncident(
                match.getId(),
                new DisciplinaryIncidentCreateRequest(999L, 500L, DisciplinaryIncidentType.AMONESTACION, 12, null)
        ));
        verify(disciplinaryIncidentRepository, never()).save(any());
    }

    private void mockContext(MatchGame match) {
        TournamentTeam home = new TournamentTeam();
        home.setId(101L);
        home.setTournamentId(match.getTournamentId());
        home.setTeamId(201L);

        TournamentTeam away = new TournamentTeam();
        away.setId(102L);
        away.setTournamentId(match.getTournamentId());
        away.setTeamId(202L);

        when(tournamentTeamRepository.findAllById(anyCollection())).thenReturn(List.of(home, away));

        Team homeTeam = new Team();
        homeTeam.setId(201L);
        homeTeam.setName("Halcones");
        homeTeam.setShortName("HAL");
        homeTeam.setCode("HAL");
        Team awayTeam = new Team();
        awayTeam.setId(202L);
        awayTeam.setName("Tigres");
        awayTeam.setShortName("TIG");
        awayTeam.setCode("TIG");
        when(teamRepository.findAllById(anyCollection())).thenReturn(List.of(homeTeam, awayTeam));

        Player player = new Player();
        player.setId(500L);
        player.setFirstName("Jugador");
        player.setLastName("Uno");
        player.setActive(true);
        when(playerRepository.findAllById(anyCollection())).thenReturn(List.of(player));
    }

    private MatchGame closedMatch(Long id, Long tournamentId, Long homeTournamentTeamId, Long awayTournamentTeamId) {
        MatchGame match = new MatchGame();
        match.setId(id);
        match.setTournamentId(tournamentId);
        match.setHomeTournamentTeamId(homeTournamentTeamId);
        match.setAwayTournamentTeamId(awayTournamentTeamId);
        match.setStatus(MatchGameStatus.PLAYED);
        match.setScheduledAt(OffsetDateTime.parse("2026-04-08T16:00:00Z"));
        return match;
    }

    private DisciplinarySanction sanction(
            Long id,
            Long incidentId,
            Long tournamentId,
            Long tournamentTeamId,
            Long playerId,
            DisciplinarySanctionType sanctionType,
            int matchesToServe
    ) {
        DisciplinarySanction sanction = new DisciplinarySanction();
        sanction.setId(id);
        sanction.setIncidentId(incidentId);
        sanction.setTournamentId(tournamentId);
        sanction.setTournamentTeamId(tournamentTeamId);
        sanction.setPlayerId(playerId);
        sanction.setSanctionType(sanctionType);
        sanction.setMatchesToServe(matchesToServe);
        sanction.setStatus(matchesToServe == 0 ? DisciplinarySanctionStatus.SERVED : DisciplinarySanctionStatus.ACTIVE);
        sanction.setCreatedAt(OffsetDateTime.parse("2026-04-08T18:00:00Z"));
        return sanction;
    }
}
