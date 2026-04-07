package com.multideporte.backend.security.auth;

import com.multideporte.backend.security.user.AppPermission;
import com.multideporte.backend.security.user.AppRole;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationCapabilityService {

    private static final List<String> BASE_READ_PERMISSIONS = List.of(
            SecurityPermissions.DASHBOARD_READ,
            SecurityPermissions.SPORTS_READ,
            SecurityPermissions.TEAMS_READ,
            SecurityPermissions.PLAYERS_READ,
            SecurityPermissions.TOURNAMENTS_READ,
            SecurityPermissions.TOURNAMENT_TEAMS_READ,
            SecurityPermissions.TOURNAMENT_STAGES_READ,
            SecurityPermissions.STAGE_GROUPS_READ,
            SecurityPermissions.ROSTERS_READ,
            SecurityPermissions.MATCHES_READ,
            SecurityPermissions.STANDINGS_READ,
            SecurityPermissions.AUTH_SESSION_READ
    );

    private static final List<String> ADMIN_MANAGE_PERMISSIONS = List.of(
            SecurityPermissions.TEAMS_MANAGE,
            SecurityPermissions.PLAYERS_MANAGE,
            SecurityPermissions.TOURNAMENTS_MANAGE,
            SecurityPermissions.TOURNAMENT_TEAMS_MANAGE,
            SecurityPermissions.TOURNAMENT_STAGES_MANAGE,
            SecurityPermissions.STAGE_GROUPS_MANAGE,
            SecurityPermissions.ROSTERS_MANAGE,
            SecurityPermissions.MATCHES_MANAGE,
            SecurityPermissions.STANDINGS_MANAGE,
            SecurityPermissions.TOURNAMENTS_STATUS_TRANSITION,
            SecurityPermissions.TOURNAMENTS_PROGRESS_TO_KNOCKOUT,
            SecurityPermissions.TOURNAMENTS_GENERATE_KNOCKOUT_BRACKET,
            SecurityPermissions.STANDINGS_RECALCULATE
    );

    private static final List<String> SUPER_ADMIN_DELETE_PERMISSIONS = List.of(
            SecurityPermissions.TEAMS_DELETE,
            SecurityPermissions.PLAYERS_DELETE,
            SecurityPermissions.TOURNAMENTS_DELETE,
            SecurityPermissions.TOURNAMENT_TEAMS_DELETE,
            SecurityPermissions.TOURNAMENT_STAGES_DELETE,
            SecurityPermissions.STAGE_GROUPS_DELETE,
            SecurityPermissions.ROSTERS_DELETE,
            SecurityPermissions.MATCHES_DELETE,
            SecurityPermissions.STANDINGS_DELETE
    );

    private static final List<String> OPERATOR_MANAGE_PERMISSIONS = List.of(
            SecurityPermissions.ROSTERS_MANAGE,
            SecurityPermissions.MATCHES_MANAGE,
            SecurityPermissions.STANDINGS_RECALCULATE
    );

    private static final Map<String, List<String>> ROLE_BASELINES = Map.of(
            "SUPER_ADMIN",
            merge(
                    BASE_READ_PERMISSIONS,
                    ADMIN_MANAGE_PERMISSIONS,
                    SUPER_ADMIN_DELETE_PERMISSIONS,
                    List.of(SecurityPermissions.OPERATIONAL_AUDIT_READ)
            ),
            "TOURNAMENT_ADMIN",
            merge(
                    BASE_READ_PERMISSIONS,
                    ADMIN_MANAGE_PERMISSIONS,
                    List.of(SecurityPermissions.OPERATIONAL_AUDIT_READ)
            ),
            "OPERATOR",
            merge(BASE_READ_PERMISSIONS, OPERATOR_MANAGE_PERMISSIONS)
    );

    public List<String> resolvePermissions(Collection<AppRole> roles) {
        Set<String> permissions = new LinkedHashSet<>();

        roles.forEach(role -> permissions.addAll(resolvePermissionsForRole(role)));

        return permissions.stream().sorted().toList();
    }

    private Set<String> resolvePermissionsForRole(AppRole role) {
        Set<String> permissionsFromDatabase = role.getPermissions().stream()
                .map(AppPermission::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        if (!permissionsFromDatabase.isEmpty()) {
            return permissionsFromDatabase;
        }

        return new LinkedHashSet<>(ROLE_BASELINES.getOrDefault(role.getCode(), List.of()));
    }

    @SafeVarargs
    private static List<String> merge(List<String>... permissionGroups) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> permissionGroup : permissionGroups) {
            merged.addAll(permissionGroup);
        }
        return List.copyOf(merged);
    }
}
