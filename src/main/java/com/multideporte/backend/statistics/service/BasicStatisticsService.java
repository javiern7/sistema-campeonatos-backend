package com.multideporte.backend.statistics.service;

import com.multideporte.backend.statistics.dto.response.BasicStatisticsResponse;

public interface BasicStatisticsService {

    BasicStatisticsResponse getBasicStatistics(Long tournamentId, Long stageId, Long groupId);
}
