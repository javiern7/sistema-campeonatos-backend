package com.multideporte.backend.stage.validation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentStageValidatorTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentStageRepository tournamentStageRepository;

    @InjectMocks
    private TournamentStageValidator tournamentStageValidator;

    @Test
    void shouldFailWhenSequenceOrderAlreadyExists() {
        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.existsByTournamentIdAndSequenceOrder(1L, 1)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                tournamentStageValidator.validateForCreate(1L, TournamentStageType.LEAGUE, 1, 1, false));
    }

    @Test
    void shouldFailWhenKnockoutRoundTripHasInvalidLegs() {
        when(tournamentRepository.existsById(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                tournamentStageValidator.validateForCreate(1L, TournamentStageType.KNOCKOUT, 1, 1, true));
    }
}
