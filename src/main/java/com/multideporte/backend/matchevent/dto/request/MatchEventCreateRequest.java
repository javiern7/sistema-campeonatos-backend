package com.multideporte.backend.matchevent.dto.request;

import com.multideporte.backend.matchevent.entity.MatchEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatchEventCreateRequest(
        @NotNull MatchEventType eventType,
        Long tournamentTeamId,
        Long playerId,
        Long relatedPlayerId,
        @Size(max = 40) String periodLabel,
        @Min(0) Integer eventMinute,
        @Min(0) @Max(59) Integer eventSecond,
        @Min(1) Integer eventValue,
        @Size(max = 500) String notes
) {
}
