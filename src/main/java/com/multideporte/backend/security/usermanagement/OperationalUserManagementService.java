package com.multideporte.backend.security.usermanagement;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusUpdateRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationalUserManagementService {

    private static final String AUDIT_RESOURCE_TYPE = "APP_USER";
    private static final String IMMUTABLE_ROLE_CODE = "SUPER_ADMIN";

    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final OperationalAuditService operationalAuditService;

    @Transactional(readOnly = true)
    public Page<OperationalUserResponse> getUsers(String query, String status, String roleCode, Pageable pageable) {
        return appUserRepository.findAll(AppUserSpecifications.byFilters(query, status, roleCode), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public OperationalUserResponse updateStatus(Long userId, OperationalUserStatusUpdateRequest request) {
        AppUser user = appUserRepository.findDetailedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        AppUserStatus newStatus = resolveStatus(request.status());
        validateManageableUser(user, request.reason());
        if (user.getStatus().equals(newStatus.name())) {
            throw new BusinessException("El usuario ya tiene ese estado");
        }

        String previousStatus = user.getStatus();
        user.setStatus(newStatus.name());
        appUserRepository.save(user);

        operationalAuditService.auditSuccess(
                "USER_STATUS_UPDATED",
                AUDIT_RESOURCE_TYPE,
                user.getId(),
                null,
                null,
                Map.of(
                        "username", user.getUsername(),
                        "previousStatus", previousStatus,
                        "newStatus", newStatus.name(),
                        "reason", request.reason().trim()
                )
        );

        return toResponse(user);
    }

    private AppUserStatus resolveStatus(String rawStatus) {
        try {
            return AppUserStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("status debe ser ACTIVE, INACTIVE o LOCKED");
        }
    }

    private void validateManageableUser(AppUser user, String reason) {
        if (user.getRoles().stream().map(AppRole::getCode).anyMatch(IMMUTABLE_ROLE_CODE::equals)) {
            operationalAuditService.auditDenied(
                    "USER_STATUS_UPDATE_DENIED",
                    AUDIT_RESOURCE_TYPE,
                    user.getId(),
                    null,
                    null,
                    "IMMUTABLE_USER",
                    Map.of("username", user.getUsername())
            );
            throw new BusinessException("El usuario no esta habilitado para gestion operativa en esta etapa");
        }

        Long currentUserId = currentUserService.getCurrentUserId().orElse(null);
        if (currentUserId != null && currentUserId.equals(user.getId())) {
            operationalAuditService.auditDenied(
                    "USER_STATUS_UPDATE_DENIED",
                    AUDIT_RESOURCE_TYPE,
                    user.getId(),
                    null,
                    null,
                    "SELF_STATUS_UPDATE_NOT_ALLOWED",
                    Map.of(
                            "username", user.getUsername(),
                            "reason", reason == null ? "" : reason.trim()
                    )
            );
            throw new BusinessException("No puedes cambiar tu propio estado en esta etapa");
        }
    }

    private OperationalUserResponse toResponse(AppUser user) {
        List<String> roles = user.getRoles().stream()
                .map(AppRole::getCode)
                .sorted()
                .toList();
        boolean statusManageable = roles.stream().noneMatch(IMMUTABLE_ROLE_CODE::equals);

        return new OperationalUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getStatus(),
                user.getLastLoginAt(),
                roles,
                statusManageable
        );
    }
}
