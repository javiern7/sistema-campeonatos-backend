package com.multideporte.backend.reporting.dto;

public record ReportExportResponse(
        String fileName,
        String contentType,
        byte[] content
) {
}
