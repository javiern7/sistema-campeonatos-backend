package com.multideporte.backend.publicportal.dto;

import java.time.OffsetDateTime;

public record PublicStandingEntryResponse(
        Long standingId,
        Integer rankPosition,
        PublicTeamSummaryResponse team,
        Integer played,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer pointsFor,
        Integer pointsAgainst,
        Integer scoreDiff,
        Integer points,
        OffsetDateTime updatedAt
) {
}
