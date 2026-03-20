package com.multideporte.backend.tournamentteam.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentTeamValidatorTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @InjectMocks
    private TournamentTeamValidator tournamentTeamValidator;

    @Test
    void shouldFailWhenTeamAlreadyRegistered() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.DRAFT);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.existsById(2L)).thenReturn(true);
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(1L, 2L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                tournamentTeamValidator.validateForCreate(1L, 2L, TournamentTeamRegistrationStatus.PENDING, 1, 1));
    }

    @Test
    void shouldFailWhenMaxTeamsReached() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setMaxTeams(2);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.existsById(2L)).thenReturn(true);
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(1L, 2L)).thenReturn(false);
        when(tournamentTeamRepository.countByTournamentIdAndRegistrationStatusIn(1L, List.of(
                TournamentTeamRegistrationStatus.PENDING,
                TournamentTeamRegistrationStatus.APPROVED
        ))).thenReturn(2L);

        assertThrows(BusinessException.class, () ->
                tournamentTeamValidator.validateForCreate(1L, 2L, TournamentTeamRegistrationStatus.PENDING, null, null));
    }
}
