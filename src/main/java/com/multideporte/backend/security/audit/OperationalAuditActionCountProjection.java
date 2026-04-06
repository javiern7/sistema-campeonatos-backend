package com.multideporte.backend.security.audit;

public record OperationalAuditActionCountProjection(
        String action,
        long total
) {
}
