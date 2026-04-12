package com.multideporte.backend.statistics.dto.response;

import java.util.List;

public record EventStatisticsTraceabilityResponse(
        boolean derivedFromMatchEvents,
        String source,
        List<String> includedEventTypes,
        List<String> excludedStatuses,
        List<String> notes
) {
}
