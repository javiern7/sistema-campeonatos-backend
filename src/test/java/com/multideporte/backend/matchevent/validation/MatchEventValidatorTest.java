package com.multideporte.backend.matchevent.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchEventValidatorTest {

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamPlayerRosterRepository teamPlayerRosterRepository;

    @InjectMocks
    private MatchEventValidator matchEventValidator;

    @Test
    void shouldFailWhenMatchIsCancelled() {
        when(matchGameRepository.findById(1L)).thenReturn(Optional.of(match(MatchGameStatus.CANCELLED)));

        assertThrows(BusinessException.class, () -> matchEventValidator.requireEditableMatch(1L));
    }

    @Test
    void shouldFailWhenScoreDoesNotProvidePlayer() {
        MatchGame match = match(MatchGameStatus.SCHEDULED);

        assertThrows(BusinessException.class, () -> matchEventValidator.validateForCreateOrUpdate(
                match,
                MatchEventType.SCORE,
                10L,
                null,
                null,
                12,
                null,
                1
        ));
    }

    @Test
    void shouldFailWhenTeamDoesNotParticipateInMatch() {
        MatchGame match = match(MatchGameStatus.PLAYED);
        TournamentTeam tournamentTeam = team(12L, 1L);
        when(tournamentTeamRepository.findById(12L)).thenReturn(Optional.of(tournamentTeam));

        assertThrows(BusinessException.class, () -> matchEventValidator.validateForCreateOrUpdate(
                match,
                MatchEventType.YELLOW_CARD,
                12L,
                20L,
                null,
                30,
                null,
                null
        ));
    }

    @Test
    void shouldFailWhenPlayerIsNotEligibleRosterMember() {
        MatchGame match = match(MatchGameStatus.SCHEDULED);
        TournamentTeam tournamentTeam = team(10L, 1L);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(tournamentTeam));
        when(playerRepository.existsById(20L)).thenReturn(true);
        when(teamPlayerRosterRepository.existsEligibleRosterMembership(10L, 20L, match.getScheduledAt().toLocalDate()))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> matchEventValidator.validateForCreateOrUpdate(
                match,
                MatchEventType.RED_CARD,
                10L,
                20L,
                null,
                44,
                null,
                null
        ));
    }

    @Test
    void shouldAcceptValidSubstitution() {
        MatchGame match = match(MatchGameStatus.PLAYED);
        TournamentTeam tournamentTeam = team(10L, 1L);
        when(tournamentTeamRepository.findById(10L)).thenReturn(Optional.of(tournamentTeam));
        when(playerRepository.existsById(20L)).thenReturn(true);
        when(playerRepository.existsById(21L)).thenReturn(true);
        when(teamPlayerRosterRepository.existsEligibleRosterMembership(10L, 20L, match.getScheduledAt().toLocalDate()))
                .thenReturn(true);
        when(teamPlayerRosterRepository.existsEligibleRosterMembership(10L, 21L, match.getScheduledAt().toLocalDate()))
                .thenReturn(true);

        assertDoesNotThrow(() -> matchEventValidator.validateForCreateOrUpdate(
                match,
                MatchEventType.SUBSTITUTION,
                10L,
                20L,
                21L,
                60,
                10,
                null
        ));
    }

    @Test
    void shouldFailWhenEditingAnnulledEvent() {
        MatchEvent event = new MatchEvent();
        event.setStatus(MatchEventStatus.ANNULLED);

        assertThrows(BusinessException.class, () -> matchEventValidator.validateForUpdate(event));
    }

    private MatchGame match(MatchGameStatus status) {
        MatchGame match = new MatchGame();
        match.setId(1L);
        match.setTournamentId(1L);
        match.setHomeTournamentTeamId(10L);
        match.setAwayTournamentTeamId(11L);
        match.setScheduledAt(OffsetDateTime.parse("2026-04-12T15:00:00Z"));
        match.setStatus(status);
        return match;
    }

    private TournamentTeam team(Long id, Long tournamentId) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setTournamentId(tournamentId);
        return tournamentTeam;
    }
}
