package com.multideporte.backend.reporting.dto;

import java.time.Instant;
import java.util.List;

public record ReportMetadataResponse(
        String reportType,
        String format,
        Instant generatedAt,
        String source,
        List<String> rules
) {
}
