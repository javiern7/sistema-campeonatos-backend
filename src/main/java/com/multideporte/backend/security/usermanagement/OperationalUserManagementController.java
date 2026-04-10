package com.multideporte.backend.security.usermanagement;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations/users")
@RequiredArgsConstructor
public class OperationalUserManagementController {

    private final OperationalUserManagementService operationalUserManagementService;

    @GetMapping
    @PreAuthorize(SecurityPermissions.CAN_READ_USERS)
    public ResponseEntity<ApiResponse<PageResponse<OperationalUserResponse>>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode,
            @PageableDefault(size = 20, sort = "username") Pageable pageable
    ) {
        Page<OperationalUserResponse> response = operationalUserManagementService.getUsers(query, status, roleCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_USERS_PAGE",
                "Usuarios operativos obtenidos correctamente",
                PageResponse.from(response)
        ));
    }

    @PutMapping("/{userId}/status")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_USERS)
    public ResponseEntity<ApiResponse<OperationalUserResponse>> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody OperationalUserStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "OPERATIONAL_USER_STATUS_UPDATED",
                "Estado de usuario actualizado correctamente",
                operationalUserManagementService.updateStatus(userId, request)
        ));
    }
}
