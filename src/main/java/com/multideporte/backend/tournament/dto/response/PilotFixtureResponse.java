package com.multideporte.backend.tournament.dto.response;

import java.util.List;

public record PilotFixtureResponse(
        Long tournamentId,
        Long stageId,
        int matchCount,
        boolean alreadyExisted,
        List<Long> matchIds
) {
}
