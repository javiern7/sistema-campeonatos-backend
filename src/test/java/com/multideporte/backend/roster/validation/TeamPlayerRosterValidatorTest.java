package com.multideporte.backend.roster.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
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
        when(tournamentTeamRepository.findById(1L)).thenReturn(java.util.Optional.of(tournamentTeam(1L, TournamentTeamRegistrationStatus.APPROVED)));
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
        when(tournamentTeamRepository.findById(1L)).thenReturn(java.util.Optional.of(tournamentTeam(1L, TournamentTeamRegistrationStatus.APPROVED)));
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

    @Test
    void shouldFailWhenActiveRosterUsesNonApprovedRegistration() {
        when(tournamentTeamRepository.findById(1L)).thenReturn(java.util.Optional.of(tournamentTeam(1L, TournamentTeamRegistrationStatus.PENDING)));
        when(playerRepository.existsById(2L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                teamPlayerRosterValidator.validateForCreate(
                        1L,
                        2L,
                        0,
                        false,
                        RosterStatus.ACTIVE,
                        LocalDate.of(2026, 3, 10),
                        null
                ));
    }

    private TournamentTeam tournamentTeam(Long id, TournamentTeamRegistrationStatus status) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setRegistrationStatus(status);
        return tournamentTeam;
    }
}
