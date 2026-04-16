package com.multideporte.backend.reporting.dto;

public record ReportTournamentResponse(
        Long id,
        String name,
        String seasonName,
        String format,
        String status,
        String operationalCategory
) {
}
