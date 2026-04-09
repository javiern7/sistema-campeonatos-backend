package com.multideporte.backend.statistics.dto.response;

public record BasicStatisticsLeaderResponse(
        String metric,
        String status,
        String scope,
        Integer value,
        Integer tieCount,
        BasicStatisticsTeamResponse team
) {
}
