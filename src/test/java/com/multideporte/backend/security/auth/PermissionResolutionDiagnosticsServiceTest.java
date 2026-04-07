package com.multideporte.backend.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.multideporte.backend.security.auth.dto.PermissionResolutionSummaryResponse;
import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionResolutionDiagnosticsServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    private final AuthorizationCapabilityService authorizationCapabilityService = new AuthorizationCapabilityService();

    @Test
    void shouldFlagRolesStillUsingFallback() {
        AppRole configuredRole = role("OPERATOR");
        configuredRole.setPermissions(Set.of(permission(SecurityPermissions.MATCHES_MANAGE)));

        AppRole fallbackRole = role("LEGACY_ROLE");

        when(appRoleRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(fallbackRole, configuredRole));

        PermissionResolutionDiagnosticsService permissionResolutionDiagnosticsService =
                new PermissionResolutionDiagnosticsService(appRoleRepository, authorizationCapabilityService);

        PermissionResolutionSummaryResponse summary = permissionResolutionDiagnosticsService.getSummary();

        assertThat(summary.fallbackActive()).isTrue();
        assertThat(summary.rolesUsingFallback()).containsExactly("LEGACY_ROLE");
        assertThat(summary.roles()).hasSize(2);
        assertThat(summary.roles().get(0).roleCode()).isEqualTo("LEGACY_ROLE");
        assertThat(summary.roles().get(0).usingFallback()).isTrue();
        assertThat(summary.roles().get(1).persistedPermissions()).containsExactly(SecurityPermissions.MATCHES_MANAGE);
    }

    private AppRole role(String code) {
        AppRole role = new AppRole();
        role.setCode(code);
        return role;
    }

    private AppPermission permission(String code) {
        AppPermission permission = new AppPermission();
        permission.setCode(code);
        permission.setName(code);
        return permission;
    }
}
