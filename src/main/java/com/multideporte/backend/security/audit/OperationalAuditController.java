package com.multideporte.backend.security.audit;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.auth.PermissionResolutionDiagnosticsService;
import com.multideporte.backend.security.audit.dto.OperationalActivitySummaryResponse;
import com.multideporte.backend.security.audit.dto.OperationalAuditEventResponse;
import com.multideporte.backend.security.auth.dto.PermissionResolutionSummaryResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationalAuditController {

    private final OperationalAuditService operationalAuditService;
    private final PermissionResolutionDiagnosticsService permissionResolutionDiagnosticsService;

    @GetMapping("/audit-events")
    @PreAuthorize(SecurityPermissions.CAN_READ_OPERATIONAL_AUDIT)
    public ResponseEntity<ApiResponse<PageResponse<OperationalAuditEventResponse>>> getAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) OperationalAuditResult result,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable
    ) {
        Page<OperationalAuditEventResponse> response = operationalAuditService.getAuditEvents(
                action,
                entityType,
                actor,
                result,
                from,
                to,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_AUDIT_EVENT_PAGE",
                "Eventos de auditoria operativa obtenidos correctamente",
                PageResponse.from(response)
        ));
    }

    @GetMapping("/audit-events/recent")
    @PreAuthorize(SecurityPermissions.CAN_READ_OPERATIONAL_AUDIT)
    public ResponseEntity<ApiResponse<List<OperationalAuditEventResponse>>> getRecentAuditEvents(
            @RequestParam(defaultValue = "10") int limit
    ) {
        int sanitizedLimit = Math.min(Math.max(limit, 1), 50);
        List<OperationalAuditEventResponse> response = operationalAuditService.getRecentAuditEvents(sanitizedLimit);

        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_AUDIT_EVENT_RECENT",
                "Eventos recientes de auditoria operativa obtenidos correctamente",
                response
        ));
    }

    @GetMapping("/activity-summary")
    @PreAuthorize(SecurityPermissions.CAN_READ_OPERATIONAL_AUDIT)
    public ResponseEntity<ApiResponse<OperationalActivitySummaryResponse>> getActivitySummary(
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to
    ) {
        OperationalActivitySummaryResponse response = operationalAuditService.getActivitySummary(from, to);

        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_ACTIVITY_SUMMARY",
                "Resumen de actividad operativa obtenido correctamente",
                response
        ));
    }

    @GetMapping("/permission-resolution-summary")
    @PreAuthorize(SecurityPermissions.CAN_READ_OPERATIONAL_AUDIT)
    public ResponseEntity<ApiResponse<PermissionResolutionSummaryResponse>> getPermissionResolutionSummary() {
        PermissionResolutionSummaryResponse response = permissionResolutionDiagnosticsService.getSummary();

        return ResponseEntity.ok(ApiResponse.success(
                "PERMISSION_RESOLUTION_SUMMARY",
                "Resumen de resolucion de permisos obtenido correctamente",
                response
        ));
    }
}
