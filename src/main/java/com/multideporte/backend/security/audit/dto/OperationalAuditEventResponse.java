package com.multideporte.backend.security.audit.dto;

import com.multideporte.backend.security.audit.OperationalAuditResult;
import java.time.OffsetDateTime;
import java.util.Map;

public record OperationalAuditEventResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        OffsetDateTime occurredAt,
        OperationalAuditResult result,
        Map<String, Object> context
) {
}
