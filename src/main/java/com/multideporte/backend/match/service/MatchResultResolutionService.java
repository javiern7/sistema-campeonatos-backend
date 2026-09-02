package com.multideporte.backend.match.service;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.entity.MatchResolutionMethod;
import com.multideporte.backend.stage.entity.TournamentStageType;
import org.springframework.stereotype.Service;

/** Resolves a closed match from its score; client supplied winners are only assertions. */
@Service
public class MatchResultResolutionService {

    public MatchResultResolution resolve(TournamentStageType stageType, MatchGameStatus status,
                                         Long homeTeamId, Long awayTeamId, Integer homeScore, Integer awayScore,
                                         Integer homePenaltyScore, Integer awayPenaltyScore,
                                         MatchResolutionMethod requestedMethod, Long requestedWinnerId) {
        boolean scoresPresent = homeScore != null && awayScore != null;
        boolean penaltiesPresent = homePenaltyScore != null || awayPenaltyScore != null;
        if (!scoresPresent || status != MatchGameStatus.PLAYED) {
            if (penaltiesPresent || requestedMethod != null) {
                throw new BusinessException("Los penales y resolutionMethod solo aplican a un partido PLAYED cerrado");
            }
            return new MatchResultResolution(requestedWinnerId, null);
        }
        if (stageType != TournamentStageType.KNOCKOUT) {
            if (penaltiesPresent || requestedMethod != null) {
                throw new BusinessException("Los partidos de grupo no admiten penales ni resolutionMethod");
            }
            return new MatchResultResolution(requestedWinnerId, null);
        }
        if (!homeScore.equals(awayScore)) {
            if (penaltiesPresent) {
                throw new BusinessException("Los penales solo corresponden a un empate reglamentario KNOCKOUT");
            }
            Long winner = homeScore > awayScore ? homeTeamId : awayTeamId;
            assertWinnerAndMethod(winner, MatchResolutionMethod.REGULATION, requestedWinnerId, requestedMethod);
            return new MatchResultResolution(winner, MatchResolutionMethod.REGULATION);
        }
        if (homePenaltyScore == null || awayPenaltyScore == null || homePenaltyScore < 0 || awayPenaltyScore < 0
                || homePenaltyScore.equals(awayPenaltyScore)) {
            throw new BusinessException("KNOCKOUT_RESOLUTION_REQUIRED: empate reglamentario requiere penales no negativos, completos y desiguales");
        }
        Long winner = homePenaltyScore > awayPenaltyScore ? homeTeamId : awayTeamId;
        assertWinnerAndMethod(winner, MatchResolutionMethod.PENALTIES, requestedWinnerId, requestedMethod);
        return new MatchResultResolution(winner, MatchResolutionMethod.PENALTIES);
    }

    private void assertWinnerAndMethod(Long expectedWinner, MatchResolutionMethod expectedMethod,
                                       Long requestedWinner, MatchResolutionMethod requestedMethod) {
        if (requestedWinner == null) {
            throw new BusinessException("KNOCKOUT_RESOLUTION_REQUIRED: winnerTournamentTeamId es obligatorio");
        }
        if (!expectedWinner.equals(requestedWinner)) {
            throw new BusinessException("WINNER_CONTRADICTS_SCORE: winnerTournamentTeamId no coincide con el resultado derivado");
        }
        if (requestedMethod != null && requestedMethod != expectedMethod) {
            throw new BusinessException("resolutionMethod no coincide con el resultado derivado");
        }
    }
}
