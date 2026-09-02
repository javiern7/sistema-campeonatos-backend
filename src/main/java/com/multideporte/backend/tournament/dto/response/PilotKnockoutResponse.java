package com.multideporte.backend.tournament.dto.response;

import java.util.List;

public record PilotKnockoutResponse(
        Long tournamentId,
        Long stageId,
        boolean alreadyExisted,
        List<Long> matchIds
) {
}
