package com.multideporte.backend.reporting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import com.multideporte.backend.matchevent.repository.MatchEventRepository;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.reporting.dto.OperationalReportResponse;
import com.multideporte.backend.reporting.dto.ReportExportResponse;
import com.multideporte.backend.reporting.dto.ScorerReportRow;
import com.multideporte.backend.reporting.service.impl.OperationalReportingServiceImpl;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalReportingServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private StandingRepository standingRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @InjectMocks
    private OperationalReportingServiceImpl reportingService;

    @Test
    void shouldBuildStandingsReportFromStoredStandingsWithoutRecalculation() {
        Standing standing = standing(30L, 10L, 22);
        TournamentTeam tournamentTeam = tournamentTeam(10L, 100L);
        Team team = team(100L, "Alpha FC");

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()));
        when(standingRepository.findAllByTournamentIdOrderByRankPositionAsc(1L)).thenReturn(List.of(standing));
        when(tournamentTeamRepository.findAllById(anyIterable())).thenReturn(List.of(tournamentTeam));
        when(teamRepository.findAllById(anyIterable())).thenReturn(List.of(team));

        var response = reportingService.getStandingsReport(1L);

        assertEquals("standings", response.metadata().reportType());
        assertEquals("standing", response.metadata().source());
        assertEquals(1, response.rows().size());
        assertEquals("Alpha FC", response.rows().get(0).teamName());
        assertEquals(22, response.rows().get(0).points());
        assertTrue(response.metadata().rules().contains("No recalcula standings"));
        verify(standingRepository).findAllByTournamentIdOrderByRankPositionAsc(1L);
    }

    @Test
    void shouldBuildScorersFromActiveScoreEventsAndExportCsv() {
        MatchEvent firstGoal = event(1L, MatchEventType.SCORE, 2);
        MatchEvent secondGoal = event(2L, MatchEventType.SCORE, 1);
        TournamentTeam tournamentTeam = tournamentTeam(10L, 100L);
        Team team = team(100L, "Alpha FC");
        Player player = player(500L, "Ana", "Goal");

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()));
        when(matchEventRepository.findReportEvents(1L, MatchEventStatus.ACTIVE, null, null, null, null))
                .thenReturn(List.of(firstGoal, secondGoal));
        when(tournamentTeamRepository.findAllById(anyIterable())).thenReturn(List.of(tournamentTeam));
        when(teamRepository.findAllById(anyIterable())).thenReturn(List.of(team));
        when(playerRepository.findAllById(anyIterable())).thenReturn(List.of(player));

        OperationalReportResponse<ScorerReportRow> response = reportingService.getScorersReport(1L, null, null);

        assertEquals(1, response.rows().size());
        assertEquals("Ana Goal", response.rows().get(0).playerName());
        assertEquals(3, response.rows().get(0).goals());

        ReportExportResponse export = reportingService.exportFile(1L, "scorers", "csv", null, null, null, null, null, null, null);
        String csv = new String(export.content(), StandardCharsets.UTF_8);
        assertEquals("text/csv; charset=UTF-8", export.contentType());
        assertTrue(csv.contains("\"Ana Goal\",\"Alpha FC\",\"3\""));

        ReportExportResponse excel = reportingService.exportFile(1L, "scorers", "xlsx", null, null, null, null, null, null, null);
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel.contentType());
        assertTrue(excel.content().length > 100);

        ReportExportResponse pdf = reportingService.exportFile(1L, "scorers", "pdf", null, null, null, null, null, null, null);
        assertEquals("application/pdf", pdf.contentType());
        assertTrue(pdf.content().length > 100);
    }

    @Test
    void shouldBuildMatchesReportWithTeamNamesAndFilters() {
        MatchGame match = match(50L, 10L, 11L);
        TournamentTeam home = tournamentTeam(10L, 100L);
        TournamentTeam away = tournamentTeam(11L, 101L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament()));
        when(matchGameRepository.findAllByTournamentIdOrderByScheduledAtAscIdAsc(1L))
                .thenReturn(List.of(match));
        when(tournamentTeamRepository.findAllById(anyIterable())).thenReturn(List.of(home, away));
        when(teamRepository.findAllById(anyIterable())).thenReturn(List.of(team(100L, "Alpha FC"), team(101L, "Beta FC")));

        var response = reportingService.getMatchesReport(1L, null, null, MatchGameStatus.PLAYED, null, null);

        assertEquals(1, response.rows().size());
        assertEquals("Alpha FC", response.rows().get(0).homeTeamName());
        assertEquals("Beta FC", response.rows().get(0).awayTeamName());
        assertEquals(MatchGameStatus.PLAYED.name(), response.rows().get(0).status());
    }

    private Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Liga Test");
        tournament.setSeasonName("2026");
        tournament.setFormat(TournamentFormat.LEAGUE);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setOperationalCategory(TournamentOperationalCategory.PRODUCTION);
        return tournament;
    }

    private Standing standing(Long id, Long tournamentTeamId, Integer points) {
        Standing standing = new Standing();
        standing.setId(id);
        standing.setTournamentId(1L);
        standing.setTournamentTeamId(tournamentTeamId);
        standing.setRankPosition(1);
        standing.setPlayed(4);
        standing.setWins(3);
        standing.setDraws(1);
        standing.setLosses(0);
        standing.setPointsFor(9);
        standing.setPointsAgainst(2);
        standing.setScoreDiff(7);
        standing.setPoints(points);
        return standing;
    }

    private MatchEvent event(Long id, MatchEventType eventType, Integer eventValue) {
        MatchEvent event = new MatchEvent();
        event.setId(id);
        event.setMatchId(50L);
        event.setTournamentId(1L);
        event.setTournamentTeamId(10L);
        event.setPlayerId(500L);
        event.setEventType(eventType);
        event.setStatus(MatchEventStatus.ACTIVE);
        event.setEventValue(eventValue);
        return event;
    }

    private MatchGame match(Long id, Long homeTournamentTeamId, Long awayTournamentTeamId) {
        MatchGame match = new MatchGame();
        match.setId(id);
        match.setTournamentId(1L);
        match.setHomeTournamentTeamId(homeTournamentTeamId);
        match.setAwayTournamentTeamId(awayTournamentTeamId);
        match.setStatus(MatchGameStatus.PLAYED);
        match.setScheduledAt(OffsetDateTime.parse("2026-04-12T10:00:00Z"));
        match.setHomeScore(2);
        match.setAwayScore(1);
        return match;
    }

    private TournamentTeam tournamentTeam(Long id, Long teamId) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setTournamentId(1L);
        tournamentTeam.setTeamId(teamId);
        return tournamentTeam;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private Player player(Long id, String firstName, String lastName) {
        Player player = new Player();
        player.setId(id);
        player.setFirstName(firstName);
        player.setLastName(lastName);
        return player;
    }
}
