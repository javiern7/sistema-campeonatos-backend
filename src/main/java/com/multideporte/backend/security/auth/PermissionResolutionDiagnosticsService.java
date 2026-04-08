package com.multideporte.backend.security.auth;

import com.multideporte.backend.security.auth.dto.PermissionResolutionSummaryResponse;
import com.multideporte.backend.security.auth.dto.RolePermissionResolutionResponse;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionResolutionDiagnosticsService {

    private final AppRoleRepository appRoleRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;

    @Transactional(readOnly = true)
    public PermissionResolutionSummaryResponse getSummary() {
        List<RolePermissionResolutionResponse> roles = appRoleRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();

        List<String> rolesUsingFallback = roles.stream()
                .filter(RolePermissionResolutionResponse::usingFallback)
                .map(RolePermissionResolutionResponse::roleCode)
                .toList();

        return new PermissionResolutionSummaryResponse(
                OffsetDateTime.now(),
                !rolesUsingFallback.isEmpty(),
                rolesUsingFallback.size(),
                rolesUsingFallback,
                roles
        );
    }

    private RolePermissionResolutionResponse toResponse(AppRole role) {
        List<String> persistedPermissions = role.getPermissions().stream()
                .map(permission -> permission.getCode())
                .sorted()
                .toList();
        List<String> effectivePermissions = authorizationCapabilityService.resolvePermissions(List.of(role));

        return new RolePermissionResolutionResponse(
                role.getCode(),
                persistedPermissions.size(),
                effectivePermissions.size(),
                persistedPermissions.isEmpty(),
                persistedPermissions,
                effectivePermissions
        );
    }
}
