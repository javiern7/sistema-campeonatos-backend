package com.multideporte.backend.security.usermanagement;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.usermanagement.dto.OperationalRoleResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationalAdministrationCatalogController {

    private final OperationalUserManagementService operationalUserManagementService;

    @GetMapping("/roles")
    @PreAuthorize(SecurityPermissions.CAN_READ_USERS)
    public ResponseEntity<ApiResponse<List<OperationalRoleResponse>>> getRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_ROLES",
                "Roles operativos obtenidos correctamente",
                operationalUserManagementService.getRoles()
        ));
    }

    @GetMapping("/user-statuses")
    @PreAuthorize(SecurityPermissions.CAN_READ_USERS)
    public ResponseEntity<ApiResponse<List<OperationalUserStatusResponse>>> getUserStatuses() {
        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_USER_STATUSES",
                "Estados operativos de usuario obtenidos correctamente",
                operationalUserManagementService.getUserStatuses()
        ));
    }
}
