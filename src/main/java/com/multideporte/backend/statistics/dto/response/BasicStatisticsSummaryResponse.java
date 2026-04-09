package com.multideporte.backend.statistics.dto.response;

import java.time.OffsetDateTime;

public record BasicStatisticsSummaryResponse(
        Long registeredTeams,
        Integer totalMatches,
        Integer playedMatches,
        Integer scheduledMatches,
        Integer forfeitMatches,
        Integer cancelledMatches,
        Integer scoredPointsFor,
        Integer scoredPointsAgainst,
        Double averagePointsPerPlayedMatch,
        OffsetDateTime lastPlayedAt
) {
}
