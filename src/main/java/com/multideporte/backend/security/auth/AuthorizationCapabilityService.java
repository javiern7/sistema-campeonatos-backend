package com.multideporte.backend.security.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationCapabilityService {

    private static final List<String> READ_RESOURCES = List.of(
            "dashboard",
            "sports",
            "teams",
            "players",
            "tournaments",
            "tournamentTeams",
            "tournamentStages",
            "stageGroups",
            "rosters",
            "matches",
            "standings"
    );

    private static final List<String> MANAGE_RESOURCES = List.of(
            "teams",
            "players",
            "tournaments",
            "tournamentTeams",
            "tournamentStages",
            "stageGroups",
            "rosters",
            "matches",
            "standings"
    );

    private static final List<String> DELETE_RESOURCES = List.of(
            "teams",
            "players",
            "tournaments",
            "tournamentTeams",
            "tournamentStages",
            "stageGroups",
            "rosters",
            "matches",
            "standings"
    );

    public List<String> resolvePermissions(Collection<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();

        READ_RESOURCES.forEach(resource -> permissions.add(permission(resource, "read")));

        if (roles.contains("SUPER_ADMIN") || roles.contains("TOURNAMENT_ADMIN")) {
            MANAGE_RESOURCES.forEach(resource -> permissions.add(permission(resource, "manage")));
        }

        if (roles.contains("SUPER_ADMIN")) {
            DELETE_RESOURCES.forEach(resource -> permissions.add(permission(resource, "delete")));
        }

        return permissions.stream().sorted().toList();
    }

    private String permission(String resource, String action) {
        return resource + ":" + action;
    }
}
