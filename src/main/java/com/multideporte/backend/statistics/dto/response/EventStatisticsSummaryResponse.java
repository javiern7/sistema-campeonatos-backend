package com.multideporte.backend.statistics.dto.response;

public record EventStatisticsSummaryResponse(
        Integer goals,
        Integer yellowCards,
        Integer redCards,
        Integer activeEvents
) {
}
