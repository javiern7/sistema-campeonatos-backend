package com.multideporte.backend.tournamentteam.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamUpdateRequest;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.mapper.TournamentTeamMapper;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRosterRepository;
import com.multideporte.backend.tournamentteam.service.impl.TournamentTeamServiceImpl;
import com.multideporte.backend.tournamentteam.validation.TournamentTeamValidator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentTeamServiceImplTest {

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TournamentTeamRosterRepository tournamentTeamRosterRepository;

    @Mock
    private TournamentTeamMapper tournamentTeamMapper;

    @Mock
    private TournamentTeamValidator tournamentTeamValidator;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private StandingRepository standingRepository;

    @InjectMocks
    private TournamentTeamServiceImpl tournamentTeamService;

    @Test
    void shouldFailWhenRegistrationLeavesApprovedAfterOperationalDataExists() {
        TournamentTeam current = new TournamentTeam();
        current.setId(7L);
        current.setTournamentId(1L);
        current.setRegistrationStatus(TournamentTeamRegistrationStatus.APPROVED);

        TournamentTeamUpdateRequest request = new TournamentTeamUpdateRequest(
                TournamentTeamRegistrationStatus.WITHDRAWN,
                0,
                0
        );

        when(tournamentTeamRepository.findById(7L)).thenReturn(Optional.of(current));
        when(tournamentTeamRosterRepository.existsByTournamentTeamId(7L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> tournamentTeamService.update(7L, request));

        verifyNoInteractions(tournamentTeamMapper);
    }
}
