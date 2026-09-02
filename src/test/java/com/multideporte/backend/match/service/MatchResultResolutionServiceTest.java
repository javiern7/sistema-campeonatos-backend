package com.multideporte.backend.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.entity.MatchResolutionMethod;
import com.multideporte.backend.stage.entity.TournamentStageType;
import org.junit.jupiter.api.Test;

class MatchResultResolutionServiceTest {

    private final MatchResultResolutionService service = new MatchResultResolutionService();

    @Test
    void derivesRegulationWinnerForKnockout() {
        MatchResultResolution result = service.resolve(TournamentStageType.KNOCKOUT, MatchGameStatus.PLAYED,
                10L, 20L, 2, 1, null, null, null, 10L);

        assertEquals(10L, result.winnerTournamentTeamId());
        assertEquals(MatchResolutionMethod.REGULATION, result.resolutionMethod());
    }

    @Test
    void derivesPenaltyWinnerWithoutChangingRegulationScore() {
        MatchResultResolution result = service.resolve(TournamentStageType.KNOCKOUT, MatchGameStatus.PLAYED,
                10L, 20L, 1, 1, 4, 5, MatchResolutionMethod.PENALTIES, 20L);

        assertEquals(20L, result.winnerTournamentTeamId());
        assertEquals(MatchResolutionMethod.PENALTIES, result.resolutionMethod());
    }

    @Test
    void rejectsPartialOrEqualPenalties() {
        assertThrows(BusinessException.class, () -> service.resolve(TournamentStageType.KNOCKOUT, MatchGameStatus.PLAYED,
                10L, 20L, 0, 0, 4, null, null, 10L));
        assertThrows(BusinessException.class, () -> service.resolve(TournamentStageType.KNOCKOUT, MatchGameStatus.PLAYED,
                10L, 20L, 0, 0, 4, 4, null, 10L));
    }

    @Test
    void rejectsWinnerThatContradictsScoreOrGroupPenalties() {
        assertThrows(BusinessException.class, () -> service.resolve(TournamentStageType.KNOCKOUT, MatchGameStatus.PLAYED,
                10L, 20L, 2, 0, null, null, null, 20L));
        assertThrows(BusinessException.class, () -> service.resolve(TournamentStageType.GROUP_STAGE, MatchGameStatus.PLAYED,
                10L, 20L, 1, 1, 4, 3, null, null));
    }
}
