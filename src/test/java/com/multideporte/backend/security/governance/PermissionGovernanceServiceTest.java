package com.multideporte.backend.security.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.governance.dto.ManagedRolePermissionResponse;
import com.multideporte.backend.security.governance.dto.RolePermissionUpdateRequest;
import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppPermissionRepository;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionGovernanceServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private OperationalAuditService operationalAuditService;

    private PermissionGovernanceProperties properties;
    private PermissionGovernanceService permissionGovernanceService;

    @BeforeEach
    void setUp() {
        properties = new PermissionGovernanceProperties();
        properties.setWriteEnabled(true);
        properties.setMutableRoles(List.of("TOURNAMENT_ADMIN", "OPERATOR"));
        permissionGovernanceService = new PermissionGovernanceService(
                appRoleRepository,
                appPermissionRepository,
                properties,
                operationalAuditService
        );
    }

    @Test
    void shouldReplacePermissionsForMutableRoleWhenGovernanceIsEnabled() {
        AppRole role = role(
                "OPERATOR",
                permission(SecurityPermissions.MATCHES_MANAGE)
        );
        when(appRoleRepository.findByCode("OPERATOR")).thenReturn(Optional.of(role));
        when(appPermissionRepository.findByCodeIn(Set.of(
                SecurityPermissions.AUTH_SESSION_READ,
                SecurityPermissions.MATCHES_MANAGE
        ))).thenReturn(List.of(
                permission(SecurityPermissions.AUTH_SESSION_READ),
                permission(SecurityPermissions.MATCHES_MANAGE)
        ));

        ManagedRolePermissionResponse response = permissionGovernanceService.replaceRolePermissions(
                "OPERATOR",
                new RolePermissionUpdateRequest(
                        List.of(SecurityPermissions.AUTH_SESSION_READ, SecurityPermissions.MATCHES_MANAGE),
                        "ajuste operativo"
                )
        );

        assertThat(response.roleCode()).isEqualTo("OPERATOR");
        assertThat(response.permissionCodes()).containsExactly(
                SecurityPermissions.AUTH_SESSION_READ,
                SecurityPermissions.MATCHES_MANAGE
        );
        verify(appRoleRepository).save(role);
        verify(operationalAuditService).auditSuccess(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectUpdatesWhenGovernanceWriteIsDisabled() {
        properties.setWriteEnabled(false);
        AppRole role = role("OPERATOR", permission(SecurityPermissions.MATCHES_MANAGE));
        when(appRoleRepository.findByCode("OPERATOR")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> permissionGovernanceService.replaceRolePermissions(
                "OPERATOR",
                new RolePermissionUpdateRequest(List.of(SecurityPermissions.MATCHES_MANAGE), "ajuste operativo")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deshabilitada");

        verify(appRoleRepository, never()).save(any());
        verify(operationalAuditService).auditDenied(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectImmutableRolesInThisStage() {
        AppRole role = role("SUPER_ADMIN", permission(SecurityPermissions.MATCHES_MANAGE));
        when(appRoleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> permissionGovernanceService.replaceRolePermissions(
                "SUPER_ADMIN",
                new RolePermissionUpdateRequest(List.of(SecurityPermissions.MATCHES_MANAGE), "ajuste operativo")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no esta habilitado");

        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownPermissions() {
        AppRole role = role("OPERATOR", permission(SecurityPermissions.MATCHES_MANAGE));
        when(appRoleRepository.findByCode("OPERATOR")).thenReturn(Optional.of(role));
        when(appPermissionRepository.findByCodeIn(Set.of("desconocido:manage"))).thenReturn(List.of());

        assertThatThrownBy(() -> permissionGovernanceService.replaceRolePermissions(
                "OPERATOR",
                new RolePermissionUpdateRequest(List.of("desconocido:manage"), "ajuste operativo")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("permisos inexistentes");

        verify(appRoleRepository, never()).save(any());
        verify(operationalAuditService).auditFailure(any(), any(), any(), any(), any(), any(), any());
    }

    private AppRole role(String code, AppPermission... permissions) {
        AppRole role = new AppRole();
        role.setCode(code);
        role.setName(code);
        role.setPermissions(Set.of(permissions));
        return role;
    }

    private AppPermission permission(String code) {
        AppPermission permission = new AppPermission();
        permission.setCode(code);
        permission.setName(code);
        return permission;
    }
}
