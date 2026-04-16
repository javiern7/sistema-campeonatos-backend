package com.multideporte.backend.reporting.dto;

import java.util.List;
import java.util.Map;

public record OperationalReportResponse<T>(
        ReportMetadataResponse metadata,
        ReportTournamentResponse tournament,
        ReportFiltersResponse filters,
        Map<String, Object> totals,
        List<T> rows
) {
}
