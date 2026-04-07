package com.multideporte.backend.security.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuthorizationCapabilityServiceTest {

    private final AuthorizationCapabilityService authorizationCapabilityService = new AuthorizationCapabilityService();

    @Test
    void shouldKeepOperationalAuditReadForAdministrativeRoles() {
        List<String> permissions = authorizationCapabilityService.resolvePermissions(List.of("TOURNAMENT_ADMIN"));

        assertThat(permissions).contains(SecurityPermissions.OPERATIONAL_AUDIT_READ);
        assertThat(permissions).contains(SecurityPermissions.DASHBOARD_READ);
    }

    @Test
    void shouldExcludeOperationalAuditReadFromOperatorBaseline() {
        List<String> permissions = authorizationCapabilityService.resolvePermissions(List.of("OPERATOR"));

        assertThat(permissions).contains(SecurityPermissions.DASHBOARD_READ);
        assertThat(permissions).contains(SecurityPermissions.AUTH_SESSION_READ);
        assertThat(permissions).doesNotContain(SecurityPermissions.OPERATIONAL_AUDIT_READ);
    }
}
