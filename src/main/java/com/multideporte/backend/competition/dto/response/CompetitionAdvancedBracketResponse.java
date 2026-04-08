package com.multideporte.backend.competition.dto.response;

import java.util.List;

public record CompetitionAdvancedBracketResponse(
        Long tournamentId,
        Long stageId,
        String stageName,
        String stageType,
        int totalMatches,
        List<BracketRound> rounds
) {

    public record BracketRound(
            Integer roundNumber,
            int matchesCount,
            List<CompetitionAdvancedMatchSummary> matches
    ) {
    }
}
