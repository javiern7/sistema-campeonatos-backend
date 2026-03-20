package com.multideporte.backend.standing.dto.response;

public record StandingRecalculationResponse(
        Long tournamentId,
        Long stageId,
        Long groupId,
        Integer matchesProcessed,
        Integer standingsGenerated
) {
}
