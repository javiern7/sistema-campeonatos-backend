package com.multideporte.backend.statistics.dto.response;

import java.util.List;

public record BasicStatisticsTraceabilityResponse(
        boolean derivedFromMatches,
        boolean derivedFromStandings,
        String classificationSource,
        List<String> notes
) {
}
