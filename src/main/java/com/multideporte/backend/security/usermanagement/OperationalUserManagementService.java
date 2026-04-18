package com.multideporte.backend.security.usermanagement;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.AuthorizationCapabilityService;
import com.multideporte.backend.security.governance.PermissionGovernanceProperties;
import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppRoleRepository;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.security.usermanagement.dto.OperationalPermissionResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalRoleResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserDetailResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserPermissionSummaryResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserRolesUpdateRequest;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusUpdateRequest;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationalUserManagementService {

    private static final String AUDIT_RESOURCE_TYPE = "APP_USER";
    private static final String IMMUTABLE_ROLE_CODE = "SUPER_ADMIN";
    private static final String IMMUTABLE_ROLE_REASON = "SUPER_ADMIN no es gestionable desde administracion operativa";
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("username"),
            Sort.Order.asc("id")
    );

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;
    private final PermissionGovernanceProperties permissionGovernanceProperties;
    private final CurrentUserService currentUserService;
    private final OperationalAuditService operationalAuditService;

    @Transactional(readOnly = true)
    public Page<OperationalUserResponse> getUsers(String query, String status, String roleCode, Pageable pageable) {
        return appUserRepository.findAll(
                        AppUserSpecifications.byFilters(query, status, roleCode),
                        normalizePageable(pageable)
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OperationalUserDetailResponse getUserDetail(Long userId) {
        AppUser user = appUserRepository.findDetailedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        return toDetailResponse(user);
    }

    @Transactional(readOnly = true)
    public OperationalUserPermissionSummaryResponse getUserPermissions(Long userId) {
        AppUser user = appUserRepository.findWithRolesAndPermissionsById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        List<OperationalPermissionResponse> permissions = authorizationCapabilityService.resolvePermissions(user.getRoles())
                .stream()
                .map(code -> resolvePermission(user.getRoles(), code))
                .sorted(Comparator.comparing(OperationalPermissionResponse::code))
                .toList();

        return new OperationalUserPermissionSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRoles().stream()
                        .map(this::toRoleResponse)
                        .sorted(Comparator.comparing(OperationalRoleResponse::roleCode))
                        .toList(),
                permissions
        );
    }

    @Transactional(readOnly = true)
    public List<OperationalRoleResponse> getRoles() {
        return appRoleRepository.findAllByOrderByCodeAsc()
                .stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OperationalUserStatusResponse> getUserStatuses() {
        return List.of(
                new OperationalUserStatusResponse(
                        AppUserStatus.ACTIVE.name(),
                        "Activo",
                        "Usuario habilitado para iniciar sesion y operar segun sus permisos"
                ),
                new OperationalUserStatusResponse(
                        AppUserStatus.INACTIVE.name(),
                        "Inactivo",
                        "Usuario deshabilitado operativamente"
                ),
                new OperationalUserStatusResponse(
                        AppUserStatus.LOCKED.name(),
                        "Bloqueado",
                        "Usuario bloqueado por decision administrativa o soporte"
                )
        );
    }

    @Transactional
    public OperationalUserResponse updateStatus(Long userId, OperationalUserStatusUpdateRequest request) {
        AppUser user = appUserRepository.findDetailedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        AppUserStatus newStatus = resolveStatus(request.status());
        validateManageableUser(user, request.reason(), "USER_STATUS_UPDATE_DENIED");
        validateNotSelfStatusUpdate(user, request.reason());
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

    @Transactional
    public OperationalUserDetailResponse replaceUserRoles(Long userId, OperationalUserRolesUpdateRequest request) {
        AppUser user = appUserRepository.findDetailedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));

        validateManageableUser(user, request.reason(), "USER_ROLES_UPDATE_DENIED");
        validateNotSelfRoleUpdate(user, request.reason());

        Set<String> requestedCodes = normalizeCodes(request.roleCodes());
        validateNoDuplicates(request.roleCodes(), requestedCodes);
        validateNoImmutableRolesRequested(user, request.reason(), requestedCodes);

        List<AppRole> roles = appRoleRepository.findByCodeIn(requestedCodes);
        validateKnownRoles(user, request.reason(), requestedCodes, roles);

        Set<String> previousCodes = extractRoleCodes(user.getRoles());
        Set<String> newCodes = extractRoleCodes(roles);
        if (previousCodes.equals(newCodes)) {
            throw new BusinessException("El usuario ya tiene exactamente ese conjunto de roles");
        }

        user.setRoles(new LinkedHashSet<>(roles));
        appUserRepository.save(user);

        Set<String> addedCodes = new LinkedHashSet<>(newCodes);
        addedCodes.removeAll(previousCodes);

        Set<String> removedCodes = new LinkedHashSet<>(previousCodes);
        removedCodes.removeAll(newCodes);

        operationalAuditService.auditSuccess(
                "USER_ROLES_UPDATED",
                AUDIT_RESOURCE_TYPE,
                user.getId(),
                null,
                null,
                new LinkedHashMap<>(Map.of(
                        "username", user.getUsername(),
                        "reason", request.reason().trim(),
                        "previousRoleCodes", List.copyOf(previousCodes),
                        "newRoleCodes", List.copyOf(newCodes),
                        "addedRoleCodes", List.copyOf(addedCodes),
                        "removedRoleCodes", List.copyOf(removedCodes)
                ))
        );

        return toDetailResponse(user);
    }

    private AppUserStatus resolveStatus(String rawStatus) {
        try {
            return AppUserStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessException("status debe ser ACTIVE, INACTIVE o LOCKED");
        }
    }

    private void validateManageableUser(AppUser user, String reason, String deniedAction) {
        if (user.getRoles().stream().map(AppRole::getCode).anyMatch(IMMUTABLE_ROLE_CODE::equals)) {
            operationalAuditService.auditDenied(
                    deniedAction,
                    AUDIT_RESOURCE_TYPE,
                    user.getId(),
                    null,
                    null,
                    "IMMUTABLE_USER",
                    Map.of("username", user.getUsername())
            );
            throw new BusinessException("El usuario no esta habilitado para gestion operativa en esta etapa");
        }
    }

    private void validateNotSelfStatusUpdate(AppUser user, String reason) {
        Long currentUserId = currentUserService.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(user.getId())) {
            return;
        }

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

    private void validateNotSelfRoleUpdate(AppUser user, String reason) {
        Long currentUserId = currentUserService.getCurrentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(user.getId())) {
            return;
        }

        operationalAuditService.auditDenied(
                "USER_ROLES_UPDATE_DENIED",
                AUDIT_RESOURCE_TYPE,
                user.getId(),
                null,
                null,
                "SELF_ROLES_UPDATE_NOT_ALLOWED",
                Map.of(
                        "username", user.getUsername(),
                        "reason", reason == null ? "" : reason.trim()
                )
        );
        throw new BusinessException("No puedes cambiar tus propios roles en esta etapa");
    }

    private void validateNoDuplicates(List<String> rawCodes, Set<String> normalizedCodes) {
        if (rawCodes.size() == normalizedCodes.size()) {
            return;
        }
        throw new BusinessException("La solicitud contiene roles duplicados");
    }

    private void validateNoImmutableRolesRequested(AppUser user, String reason, Set<String> requestedCodes) {
        if (!requestedCodes.contains(IMMUTABLE_ROLE_CODE)) {
            return;
        }
        operationalAuditService.auditDenied(
                "USER_ROLES_UPDATE_DENIED",
                AUDIT_RESOURCE_TYPE,
                user.getId(),
                null,
                null,
                "IMMUTABLE_ROLE_REQUESTED",
                Map.of(
                        "username", user.getUsername(),
                        "requestedRoleCodes", List.copyOf(requestedCodes),
                        "reason", reason == null ? "" : reason.trim()
                )
        );
        throw new BusinessException("SUPER_ADMIN no puede asignarse desde administracion operativa en esta etapa");
    }

    private void validateKnownRoles(
            AppUser user,
            String reason,
            Set<String> requestedCodes,
            List<AppRole> resolvedRoles
    ) {
        Set<String> resolvedCodes = extractRoleCodes(resolvedRoles);
        if (requestedCodes.equals(resolvedCodes)) {
            return;
        }

        Set<String> unknownCodes = new LinkedHashSet<>(requestedCodes);
        unknownCodes.removeAll(resolvedCodes);
        operationalAuditService.auditFailure(
                "USER_ROLES_UPDATE_FAILED",
                AUDIT_RESOURCE_TYPE,
                user.getId(),
                null,
                null,
                "UNKNOWN_ROLE_CODES",
                Map.of(
                        "username", user.getUsername(),
                        "unknownRoleCodes", List.copyOf(unknownCodes),
                        "reason", reason == null ? "" : reason.trim()
                )
        );
        throw new BusinessException("La solicitud contiene roles inexistentes: " + String.join(", ", unknownCodes));
    }

    private OperationalUserDetailResponse toDetailResponse(AppUser user) {
        List<OperationalRoleResponse> roles = user.getRoles().stream()
                .map(this::toRoleResponse)
                .sorted(Comparator.comparing(OperationalRoleResponse::roleCode))
                .toList();

        boolean manageable = isManageableUser(user);
        String manageabilityReason = manageable ? null : IMMUTABLE_ROLE_REASON;

        return new OperationalUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getStatus(),
                user.getLastLoginAt(),
                roles,
                manageable,
                manageabilityReason,
                manageable,
                manageabilityReason
        );
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

    private OperationalRoleResponse toRoleResponse(AppRole role) {
        boolean immutable = IMMUTABLE_ROLE_CODE.equals(role.getCode());
        boolean mutable = normalizeCodes(permissionGovernanceProperties.getMutableRoles()).contains(role.getCode());
        return new OperationalRoleResponse(
                role.getCode(),
                role.getName(),
                role.getDescription(),
                mutable,
                !immutable,
                immutable ? IMMUTABLE_ROLE_REASON : null
        );
    }

    private OperationalPermissionResponse resolvePermission(Collection<AppRole> roles, String code) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> code.equals(permission.getCode()))
                .findFirst()
                .map(this::toPermissionResponse)
                .orElseGet(() -> new OperationalPermissionResponse(code, code, null));
    }

    private OperationalPermissionResponse toPermissionResponse(AppPermission permission) {
        return new OperationalPermissionResponse(
                permission.getCode(),
                permission.getName(),
                permission.getDescription()
        );
    }

    private boolean isManageableUser(AppUser user) {
        return user.getRoles().stream()
                .map(AppRole::getCode)
                .noneMatch(IMMUTABLE_ROLE_CODE::equals);
    }

    private Set<String> extractRoleCodes(Collection<AppRole> roles) {
        return roles.stream()
                .map(AppRole::getCode)
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

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, DEFAULT_SORT);
        }

        Sort normalizedSort = pageable.getSort().isSorted()
                ? translateSort(pageable.getSort())
                : DEFAULT_SORT;

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private Sort translateSort(Sort requestedSort) {
        List<Sort.Order> translatedOrders = requestedSort.stream()
                .flatMap(order -> mapSortOrder(order).stream())
                .toList();

        if (translatedOrders.isEmpty()) {
            return DEFAULT_SORT;
        }

        boolean containsId = translatedOrders.stream()
                .anyMatch(order -> "id".equals(order.getProperty()));

        return containsId
                ? Sort.by(translatedOrders)
                : Sort.by(translatedOrders).and(Sort.by(new Sort.Order(Sort.Direction.ASC, "id")));
    }

    private List<Sort.Order> mapSortOrder(Sort.Order order) {
        Sort.Direction direction = order.getDirection();

        return switch (order.getProperty()) {
            case "userId", "id" -> List.of(new Sort.Order(direction, "id"));
            case "username" -> List.of(new Sort.Order(direction, "username"));
            case "email" -> List.of(new Sort.Order(direction, "email"));
            case "firstName" -> List.of(new Sort.Order(direction, "firstName"));
            case "lastName" -> List.of(new Sort.Order(direction, "lastName"));
            case "fullName" -> List.of(
                    new Sort.Order(direction, "firstName"),
                    new Sort.Order(direction, "lastName")
            );
            case "status" -> List.of(new Sort.Order(direction, "status"));
            case "lastLoginAt" -> List.of(new Sort.Order(direction, "lastLoginAt"));
            default -> List.of();
        };
    }
}
