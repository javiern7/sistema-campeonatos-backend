package com.multideporte.backend.reporting.dto;

import java.time.OffsetDateTime;

public record EventReportRow(
        Long eventId,
        Long matchId,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        String eventType,
        String status,
        String periodLabel,
        Integer eventMinute,
        Integer eventSecond,
        Integer eventValue,
        OffsetDateTime createdAt
) {
}
