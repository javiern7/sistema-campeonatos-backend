package com.multideporte.backend.matchevent.dto.response;

import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import java.time.OffsetDateTime;

public record MatchEventResponse(
        Long id,
        Long matchId,
        Long tournamentId,
        MatchEventType eventType,
        MatchEventStatus status,
        Long tournamentTeamId,
        Long playerId,
        Long relatedPlayerId,
        String periodLabel,
        Integer eventMinute,
        Integer eventSecond,
        Integer eventValue,
        String notes,
        Long createdByUserId,
        Long annulledByUserId,
        OffsetDateTime annulledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
