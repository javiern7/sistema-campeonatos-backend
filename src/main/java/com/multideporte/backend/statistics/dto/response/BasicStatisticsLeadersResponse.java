package com.multideporte.backend.statistics.dto.response;

public record BasicStatisticsLeadersResponse(
        BasicStatisticsLeaderResponse pointsLeader,
        BasicStatisticsLeaderResponse winsLeader,
        BasicStatisticsLeaderResponse scoreDiffLeader,
        BasicStatisticsLeaderResponse scoringLeader
) {
}
