package com.multideporte.backend.statistics.dto.response;

public record BasicStatisticsResponse(
        Long tournamentId,
        Long stageId,
        Long groupId,
        BasicStatisticsSummaryResponse summary,
        BasicStatisticsLeadersResponse leaders,
        BasicStatisticsTraceabilityResponse traceability
) {
}
