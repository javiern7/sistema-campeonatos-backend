package com.multideporte.backend.stage.dto.response;

import com.multideporte.backend.stage.entity.TournamentStageType;
import java.time.OffsetDateTime;

public record TournamentStageResponse(
        Long id,
        Long tournamentId,
        String name,
        TournamentStageType stageType,
        Integer sequenceOrder,
        Integer legs,
        Boolean roundTrip,
        Boolean active,
        OffsetDateTime createdAt
) {
}
