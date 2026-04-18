package com.multideporte.backend.security.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.AuthorizationCapabilityService;
import com.multideporte.backend.security.governance.PermissionGovernanceProperties;
import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserDetailResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserPermissionSummaryResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserRolesUpdateRequest;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusUpdateRequest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OperationalUserManagementServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AuthorizationCapabilityService authorizationCapabilityService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OperationalAuditService operationalAuditService;

    private OperationalUserManagementService operationalUserManagementService;

    @BeforeEach
    void setUp() {
        PermissionGovernanceProperties permissionGovernanceProperties = new PermissionGovernanceProperties();
        permissionGovernanceProperties.setMutableRoles(java.util.List.of("TOURNAMENT_ADMIN", "OPERATOR"));
        operationalUserManagementService = new OperationalUserManagementService(
                appUserRepository,
                appRoleRepository,
                authorizationCapabilityService,
                permissionGovernanceProperties,
                currentUserService,
                operationalAuditService
        );
    }

    @Test
    void shouldUpdateStatusForManageableUser() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));

        OperationalUserResponse response = operationalUserManagementService.updateStatus(
                20L,
                new OperationalUserStatusUpdateRequest("LOCKED", "bloqueo operativo")
        );

        assertThat(response.status()).isEqualTo("LOCKED");
        verify(appUserRepository).save(user);
        verify(operationalAuditService).auditSuccess(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectSelfStatusUpdate() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));

        assertThatThrownBy(() -> operationalUserManagementService.updateStatus(
                20L,
                new OperationalUserStatusUpdateRequest("LOCKED", "bloqueo operativo")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propio estado");
    }

    @Test
    void shouldRejectSuperAdminStatusChanges() {
        AppUser user = user(10L, "admin", "ACTIVE", "SUPER_ADMIN");
        when(appUserRepository.findDetailedById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> operationalUserManagementService.updateStatus(
                10L,
                new OperationalUserStatusUpdateRequest("LOCKED", "bloqueo operativo")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("gestion operativa");
    }

    @Test
    void shouldReturnUserDetailWithRoleManageabilityMetadata() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));

        OperationalUserDetailResponse response = operationalUserManagementService.getUserDetail(20L);

        assertThat(response.userId()).isEqualTo(20L);
        assertThat(response.rolesManageable()).isTrue();
        assertThat(response.roles()).extracting("roleCode").containsExactly("OPERATOR");
    }

    @Test
    void shouldReturnEffectiveUserPermissionsWithMetadata() {
        AppPermission permission = permission("matches:manage", "Matches manage", "Permite gestionar partidos");
        AppRole role = role("OPERATOR");
        role.setPermissions(Set.of(permission));
        AppUser user = user(20L, "operator", "ACTIVE");
        user.setRoles(Set.of(role));
        when(appUserRepository.findWithRolesAndPermissionsById(20L)).thenReturn(Optional.of(user));
        when(authorizationCapabilityService.resolvePermissions(user.getRoles())).thenReturn(java.util.List.of("matches:manage"));

        OperationalUserPermissionSummaryResponse response = operationalUserManagementService.getUserPermissions(20L);

        assertThat(response.permissions()).extracting("code").containsExactly("matches:manage");
        assertThat(response.permissions().get(0).description()).isEqualTo("Permite gestionar partidos");
    }

    @Test
    void shouldReplaceUserRolesForManageableUser() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        AppRole newRole = role("TOURNAMENT_ADMIN");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));
        when(appRoleRepository.findByCodeIn(Set.of("TOURNAMENT_ADMIN"))).thenReturn(java.util.List.of(newRole));

        OperationalUserDetailResponse response = operationalUserManagementService.replaceUserRoles(
                20L,
                new OperationalUserRolesUpdateRequest(java.util.List.of("TOURNAMENT_ADMIN"), "ascenso operativo")
        );

        assertThat(response.roles()).extracting("roleCode").containsExactly("TOURNAMENT_ADMIN");
        verify(appUserRepository).save(user);
        verify(operationalAuditService).auditSuccess(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectSuperAdminRoleAssignment() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> operationalUserManagementService.replaceUserRoles(
                20L,
                new OperationalUserRolesUpdateRequest(java.util.List.of("SUPER_ADMIN"), "ascenso no permitido")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SUPER_ADMIN");
    }

    @Test
    void shouldRejectSelfRoleUpdate() {
        AppUser user = user(20L, "operator", "ACTIVE", "OPERATOR");
        when(appUserRepository.findDetailedById(20L)).thenReturn(Optional.of(user));
        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(20L));

        assertThatThrownBy(() -> operationalUserManagementService.replaceUserRoles(
                20L,
                new OperationalUserRolesUpdateRequest(java.util.List.of("TOURNAMENT_ADMIN"), "autocambio no permitido")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propios roles");
    }
    @Test
    void shouldTranslateFullNameSortToEntityFields() {
        when(appUserRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        operationalUserManagementService.getUsers(
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Order.asc("fullName")))
        );

        verify(appUserRepository).findAll(anySpecification(), eq(PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.asc("firstName"),
                        Sort.Order.asc("lastName"),
                        Sort.Order.asc("id")
                )
        )));
    }

    @Test
    void shouldFallbackToDefaultSortWhenSortIsUnsupported() {
        when(appUserRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        operationalUserManagementService.getUsers(
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("unsupportedField")))
        );

        verify(appUserRepository).findAll(anySpecification(), eq(PageRequest.of(
                0,
                20,
                Sort.by(
                        Sort.Order.asc("username"),
                        Sort.Order.asc("id")
                )
        )));
    }

    private Specification<AppUser> anySpecification() {
        return any();
    }

    private AppUser user(Long id, String username, String status, String... roleCodes) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@local.test");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(status);
        user.setRoles(Set.of(java.util.Arrays.stream(roleCodes)
                .map(this::role)
                .toArray(AppRole[]::new)));
        return user;
    }

    private AppRole role(String code) {
        AppRole role = new AppRole();
        role.setCode(code);
        role.setName(code);
        role.setDescription(code + " description");
        return role;
    }

    private AppPermission permission(String code, String name, String description) {
        AppPermission permission = new AppPermission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        return permission;
    }
}




