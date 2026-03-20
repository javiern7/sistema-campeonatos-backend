package com.multideporte.backend.roster.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamPlayerRosterValidatorTest {

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamPlayerRosterRepository teamPlayerRosterRepository;

    @InjectMocks
    private TeamPlayerRosterValidator teamPlayerRosterValidator;

    @Test
    void shouldFailWhenEndDateIsBeforeStartDate() {
        when(tournamentTeamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.existsById(2L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                teamPlayerRosterValidator.validateForCreate(
                        1L,
                        2L,
                        10,
                        false,
                        RosterStatus.ACTIVE,
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2026, 3, 9)
                ));
    }

    @Test
    void shouldFailWhenActiveCaptainAlreadyExists() {
        when(tournamentTeamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.existsById(2L)).thenReturn(true);
        when(teamPlayerRosterRepository.existsByTournamentTeamIdAndCaptainTrueAndRosterStatus(1L, RosterStatus.ACTIVE))
                .thenReturn(true);

        assertThrows(BusinessException.class, () ->
                teamPlayerRosterValidator.validateForCreate(
                        1L,
                        2L,
                        10,
                        true,
                        RosterStatus.ACTIVE,
                        LocalDate.of(2026, 3, 10),
                        null
                ));
    }
}
