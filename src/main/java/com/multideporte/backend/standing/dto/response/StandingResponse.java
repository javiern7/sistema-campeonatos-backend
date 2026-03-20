package com.multideporte.backend.standing.dto.response;

import java.time.OffsetDateTime;

public record StandingResponse(
        Long id,
        Long tournamentId,
        Long stageId,
        Long groupId,
        Long tournamentTeamId,
        Integer played,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer pointsFor,
        Integer pointsAgainst,
        Integer scoreDiff,
        Integer points,
        Integer rankPosition,
        OffsetDateTime updatedAt
) {
}
