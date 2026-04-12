package com.multideporte.backend.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import com.multideporte.backend.matchevent.repository.MatchEventRepository;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.statistics.dto.response.EventStatisticsResponse;
import com.multideporte.backend.statistics.service.impl.EventStatisticsServiceImpl;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventStatisticsServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private MatchEventRepository matchEventRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private EventStatisticsServiceImpl eventStatisticsService;

    @Test
    void shouldBuildDerivedStatisticsFromActiveMatchEvents() {
        MatchGame match = match(50L, 1L, 10L, 11L);
        TournamentTeam tournamentTeam = tournamentTeam(10L, 100L);
        Team team = team(100L, "Alpha FC", "ALP");
        Player player = player(500L, "Ana", "Goal");

        MatchEvent goal = event(1L, 50L, 1L, 10L, 500L, MatchEventType.SCORE, 2);
        MatchEvent yellow = event(2L, 50L, 1L, 10L, 500L, MatchEventType.YELLOW_CARD, null);
        MatchEvent red = event(3L, 50L, 1L, 10L, 500L, MatchEventType.RED_CARD, null);
        MatchEvent note = event(4L, 50L, 1L, null, null, MatchEventType.NOTE, null);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(matchEventRepository.findActiveDerivedStatisticsEvents(1L, MatchEventStatus.ACTIVE, null, null, null))
                .thenReturn(List.of(goal, yellow, red, note));
        when(matchGameRepository.findAllById(anyIterable())).thenReturn(List.of(match));
        when(tournamentTeamRepository.findAllById(anyIterable())).thenReturn(List.of(tournamentTeam));
        when(teamRepository.findAllById(anyIterable())).thenReturn(List.of(team));
        when(playerRepository.findAllById(anyIterable())).thenReturn(List.of(player));

        EventStatisticsResponse response = eventStatisticsService.getEventStatistics(1L, null, null, null, null);

        assertEquals(2, response.summary().goals());
        assertEquals(1, response.summary().yellowCards());
        assertEquals(1, response.summary().redCards());
        assertEquals(4, response.summary().activeEvents());
        assertEquals(2, response.players().get(0).goals());
        assertEquals("Ana Goal", response.players().get(0).displayName());
        assertEquals(2, response.teams().get(0).goals());
        assertEquals(4, response.matches().get(0).activeEvents());
        assertEquals(List.of("ANNULLED"), response.traceability().excludedStatuses());
        verify(matchEventRepository).findActiveDerivedStatisticsEvents(1L, MatchEventStatus.ACTIVE, null, null, null);
    }

    @Test
    void shouldResolveTeamIdToTournamentTeamFilter() {
        TournamentTeam tournamentTeam = tournamentTeam(10L, 100L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(teamRepository.existsById(100L)).thenReturn(true);
        when(tournamentTeamRepository.findByTournamentIdAndTeamId(1L, 100L)).thenReturn(Optional.of(tournamentTeam));
        when(matchEventRepository.findActiveDerivedStatisticsEvents(1L, MatchEventStatus.ACTIVE, null, 10L, null))
                .thenReturn(List.of());

        EventStatisticsResponse response = eventStatisticsService.getEventStatistics(1L, null, null, 100L, null);

        assertEquals(10L, response.filters().tournamentTeamId());
        assertEquals(100L, response.filters().teamId());
        verify(matchEventRepository).findActiveDerivedStatisticsEvents(1L, MatchEventStatus.ACTIVE, null, 10L, null);
    }

    @Test
    void shouldRejectTeamThatDoesNotParticipateInMatchFilter() {
        MatchGame match = match(50L, 1L, 10L, 11L);
        TournamentTeam tournamentTeam = tournamentTeam(99L, 999L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(matchGameRepository.findById(50L)).thenReturn(Optional.of(match));
        when(tournamentTeamRepository.findById(99L)).thenReturn(Optional.of(tournamentTeam));

        assertThrows(BusinessException.class, () -> eventStatisticsService.getEventStatistics(1L, 50L, 99L, null, null));
    }

    private MatchEvent event(
            Long id,
            Long matchId,
            Long tournamentId,
            Long tournamentTeamId,
            Long playerId,
            MatchEventType eventType,
            Integer eventValue
    ) {
        MatchEvent event = new MatchEvent();
        event.setId(id);
        event.setMatchId(matchId);
        event.setTournamentId(tournamentId);
        event.setTournamentTeamId(tournamentTeamId);
        event.setPlayerId(playerId);
        event.setEventType(eventType);
        event.setStatus(MatchEventStatus.ACTIVE);
        event.setEventValue(eventValue);
        return event;
    }

    private MatchGame match(Long id, Long tournamentId, Long homeTournamentTeamId, Long awayTournamentTeamId) {
        MatchGame match = new MatchGame();
        match.setId(id);
        match.setTournamentId(tournamentId);
        match.setHomeTournamentTeamId(homeTournamentTeamId);
        match.setAwayTournamentTeamId(awayTournamentTeamId);
        match.setScheduledAt(OffsetDateTime.parse("2026-04-12T10:00:00Z"));
        return match;
    }

    private TournamentTeam tournamentTeam(Long id, Long teamId) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setTournamentId(1L);
        tournamentTeam.setTeamId(teamId);
        tournamentTeam.setSeedNumber(7);
        return tournamentTeam;
    }

    private Team team(Long id, String name, String shortName) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setShortName(shortName);
        team.setCode(shortName);
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
