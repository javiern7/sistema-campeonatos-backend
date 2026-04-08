package com.multideporte.backend.security.governance;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.governance.dto.ManagedPermissionResponse;
import com.multideporte.backend.security.governance.dto.ManagedRolePermissionResponse;
import com.multideporte.backend.security.governance.dto.PermissionGovernanceSummaryResponse;
import com.multideporte.backend.security.governance.dto.RolePermissionUpdateRequest;
import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppPermissionRepository;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionGovernanceService {

    private static final String AUDIT_RESOURCE_TYPE = "APP_ROLE_PERMISSION";

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final PermissionGovernanceProperties properties;
    private final OperationalAuditService operationalAuditService;

    @Transactional(readOnly = true)
    public PermissionGovernanceSummaryResponse getSummary() {
        List<ManagedPermissionResponse> availablePermissions = appPermissionRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(permission -> new ManagedPermissionResponse(
                        permission.getCode(),
                        permission.getName(),
                        permission.getDescription()
                ))
                .toList();

        List<ManagedRolePermissionResponse> roles = appRoleRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toRoleResponse)
                .toList();

        return new PermissionGovernanceSummaryResponse(
                OffsetDateTime.now(),
                properties.isWriteEnabled(),
                List.copyOf(normalizeCodes(properties.getMutableRoles())),
                availablePermissions,
                roles
        );
    }

    @Transactional
    public ManagedRolePermissionResponse replaceRolePermissions(String roleCode, RolePermissionUpdateRequest request) {
        AppRole role = appRoleRepository.findByCode(normalizeCode(roleCode))
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleCode));

        validateRoleIsMutable(role);
        validateWriteEnabled(role.getCode(), request.reason());

        Set<String> requestedCodes = normalizeCodes(request.permissionCodes());
        validateNoDuplicates(request.permissionCodes(), requestedCodes);

        List<AppPermission> permissions = appPermissionRepository.findByCodeIn(requestedCodes);
        validateKnownPermissions(role.getCode(), request.reason(), requestedCodes, permissions);

        Set<String> previousCodes = extractPermissionCodes(role.getPermissions());
        Set<String> newCodes = extractPermissionCodes(permissions);
        if (previousCodes.equals(newCodes)) {
            throw new BusinessException("El rol ya tiene exactamente ese conjunto de permisos");
        }

        role.setPermissions(new LinkedHashSet<>(permissions));
        appRoleRepository.save(role);

        Set<String> addedCodes = new LinkedHashSet<>(newCodes);
        addedCodes.removeAll(previousCodes);

        Set<String> removedCodes = new LinkedHashSet<>(previousCodes);
        removedCodes.removeAll(newCodes);

        operationalAuditService.auditSuccess(
                "PERMISSION_ROLE_ASSIGNMENTS_UPDATED",
                AUDIT_RESOURCE_TYPE,
                role.getCode(),
                null,
                null,
                new LinkedHashMap<>(Map.of(
                        "roleCode", role.getCode(),
                        "reason", request.reason().trim(),
                        "previousPermissionCodes", List.copyOf(previousCodes),
                        "newPermissionCodes", List.copyOf(newCodes),
                        "addedPermissionCodes", List.copyOf(addedCodes),
                        "removedPermissionCodes", List.copyOf(removedCodes),
                        "writeEnabled", properties.isWriteEnabled()
                ))
        );

        return toRoleResponse(role);
    }

    private void validateRoleIsMutable(AppRole role) {
        if (!normalizeCodes(properties.getMutableRoles()).contains(normalizeCode(role.getCode()))) {
            operationalAuditService.auditDenied(
                    "PERMISSION_ROLE_ASSIGNMENTS_UPDATE_DENIED",
                    AUDIT_RESOURCE_TYPE,
                    role.getCode(),
                    null,
                    null,
                    "IMMUTABLE_ROLE",
                    Map.of("roleCode", role.getCode())
            );
            throw new BusinessException("El rol no esta habilitado para administracion operativa en esta etapa");
        }
    }

    private void validateWriteEnabled(String roleCode, String reason) {
        if (properties.isWriteEnabled()) {
            return;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("roleCode", roleCode);
        if (reason != null && !reason.isBlank()) {
            context.put("reason", reason.trim());
        }
        operationalAuditService.auditDenied(
                "PERMISSION_ROLE_ASSIGNMENTS_UPDATE_DENIED",
                AUDIT_RESOURCE_TYPE,
                roleCode,
                null,
                null,
                "WRITE_DISABLED",
                context
        );
        throw new BusinessException("La administracion operativa de permisos esta deshabilitada en este ambiente");
    }

    private void validateNoDuplicates(List<String> rawCodes, Set<String> normalizedCodes) {
        if (rawCodes.size() == normalizedCodes.size()) {
            return;
        }
        throw new BusinessException("La solicitud contiene permisos duplicados");
    }

    private void validateKnownPermissions(
            String roleCode,
            String reason,
            Set<String> requestedCodes,
            List<AppPermission> resolvedPermissions
    ) {
        Set<String> resolvedCodes = extractPermissionCodes(resolvedPermissions);
        if (requestedCodes.equals(resolvedCodes)) {
            return;
        }

        Set<String> unknownCodes = new LinkedHashSet<>(requestedCodes);
        unknownCodes.removeAll(resolvedCodes);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("roleCode", roleCode);
        context.put("unknownPermissionCodes", List.copyOf(unknownCodes));
        if (reason != null && !reason.isBlank()) {
            context.put("reason", reason.trim());
        }
        operationalAuditService.auditFailure(
                "PERMISSION_ROLE_ASSIGNMENTS_UPDATE_FAILED",
                AUDIT_RESOURCE_TYPE,
                roleCode,
                null,
                null,
                "UNKNOWN_PERMISSION_CODES",
                context
        );
        throw new BusinessException("La solicitud contiene permisos inexistentes: " + String.join(", ", unknownCodes));
    }

    private ManagedRolePermissionResponse toRoleResponse(AppRole role) {
        return new ManagedRolePermissionResponse(
                role.getCode(),
                role.getName(),
                normalizeCodes(properties.getMutableRoles()).contains(normalizeCode(role.getCode())),
                role.getPermissions().stream()
                        .map(AppPermission::getCode)
                        .sorted()
                        .toList()
        );
    }

    private Set<String> extractPermissionCodes(Collection<AppPermission> permissions) {
        return permissions.stream()
                .map(AppPermission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeCodes(Collection<String> codes) {
        return codes.stream()
                .map(this::normalizeCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }
}
