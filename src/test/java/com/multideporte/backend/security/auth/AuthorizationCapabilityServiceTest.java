package com.multideporte.backend.security.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppRole;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthorizationCapabilityServiceTest {

    private final AuthorizationCapabilityService authorizationCapabilityService = new AuthorizationCapabilityService();

    @Test
    void shouldKeepOperationalAuditReadForAdministrativeRoles() {
        List<String> permissions = authorizationCapabilityService.resolvePermissions(List.of(role("TOURNAMENT_ADMIN")));

        assertThat(permissions).contains(SecurityPermissions.OPERATIONAL_AUDIT_READ);
        assertThat(permissions).contains(SecurityPermissions.DASHBOARD_READ);
    }

    @Test
    void shouldExcludeOperationalAuditReadFromOperatorBaseline() {
        List<String> permissions = authorizationCapabilityService.resolvePermissions(List.of(role("OPERATOR")));

        assertThat(permissions).contains(SecurityPermissions.DASHBOARD_READ);
        assertThat(permissions).contains(SecurityPermissions.AUTH_SESSION_READ);
        assertThat(permissions).doesNotContain(SecurityPermissions.OPERATIONAL_AUDIT_READ);
    }

    @Test
    void shouldPreferDatabasePermissionsWhenRoleAssignmentExists() {
        AppRole role = role("OPERATOR");
        role.setPermissions(Set.of(permission(SecurityPermissions.MATCHES_MANAGE)));

        List<String> permissions = authorizationCapabilityService.resolvePermissions(List.of(role));

        assertThat(permissions).containsExactly(SecurityPermissions.MATCHES_MANAGE);
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
