package com.multideporte.backend.security.governance;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.governance.dto.ManagedRolePermissionResponse;
import com.multideporte.backend.security.governance.dto.PermissionGovernanceSummaryResponse;
import com.multideporte.backend.security.governance.dto.RolePermissionUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations/permission-governance")
@RequiredArgsConstructor
public class PermissionGovernanceController {

    private final PermissionGovernanceService permissionGovernanceService;

    @GetMapping("/roles")
    @PreAuthorize(SecurityPermissions.CAN_READ_OPERATIONAL_AUDIT)
    public ResponseEntity<ApiResponse<PermissionGovernanceSummaryResponse>> getSummary() {
        PermissionGovernanceSummaryResponse response = permissionGovernanceService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(
                "PERMISSION_GOVERNANCE_SUMMARY",
                "Resumen de gobierno operativo de permisos obtenido correctamente",
                response
        ));
    }

    @PutMapping("/roles/{roleCode}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_PERMISSION_GOVERNANCE)
    public ResponseEntity<ApiResponse<ManagedRolePermissionResponse>> replaceRolePermissions(
            @PathVariable String roleCode,
            @Valid @RequestBody RolePermissionUpdateRequest request
    ) {
        ManagedRolePermissionResponse response = permissionGovernanceService.replaceRolePermissions(roleCode, request);
        return ResponseEntity.ok(ApiResponse.success(
                "PERMISSION_ROLE_ASSIGNMENTS_UPDATED",
                "Permisos del rol actualizados correctamente",
                response
        ));
    }
}
