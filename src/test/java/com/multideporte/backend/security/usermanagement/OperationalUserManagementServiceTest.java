package com.multideporte.backend.security.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusUpdateRequest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationalUserManagementServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OperationalAuditService operationalAuditService;

    private OperationalUserManagementService operationalUserManagementService;

    @BeforeEach
    void setUp() {
        operationalUserManagementService = new OperationalUserManagementService(
                appUserRepository,
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
        return role;
    }
}
