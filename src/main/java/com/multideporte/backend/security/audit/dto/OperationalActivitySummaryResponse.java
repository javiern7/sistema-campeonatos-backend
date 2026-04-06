package com.multideporte.backend.security.audit.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OperationalActivitySummaryResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        long totalEvents,
        long successEvents,
        long deniedEvents,
        long failedEvents,
        long uniqueActors,
        List<ActionCountResponse> topActions
) {

    public record ActionCountResponse(
            String action,
            long total
    ) {
    }
}
