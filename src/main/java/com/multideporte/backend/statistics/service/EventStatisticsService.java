package com.multideporte.backend.statistics.service;

import com.multideporte.backend.statistics.dto.response.EventStatisticsResponse;

public interface EventStatisticsService {

    EventStatisticsResponse getEventStatistics(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId
    );
}
