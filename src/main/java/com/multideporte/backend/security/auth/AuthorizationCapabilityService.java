package com.multideporte.backend.security.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
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
            SecurityPermissions.AUTH_SESSION_READ,
            SecurityPermissions.OPERATIONAL_AUDIT_READ
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

    public List<String> resolvePermissions(Collection<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();

        permissions.addAll(BASE_READ_PERMISSIONS);

        if (roles.contains("SUPER_ADMIN") || roles.contains("TOURNAMENT_ADMIN")) {
            permissions.addAll(ADMIN_MANAGE_PERMISSIONS);
        }

        if (roles.contains("OPERATOR")) {
            permissions.addAll(OPERATOR_MANAGE_PERMISSIONS);
        }

        if (roles.contains("SUPER_ADMIN")) {
            permissions.addAll(SUPER_ADMIN_DELETE_PERMISSIONS);
        }

        return permissions.stream().sorted().toList();
    }
}
