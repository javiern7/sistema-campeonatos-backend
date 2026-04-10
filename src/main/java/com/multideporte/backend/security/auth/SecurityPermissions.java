package com.multideporte.backend.security.auth;

public final class SecurityPermissions {

    private SecurityPermissions() {
    }

    public static final String DASHBOARD_READ = "dashboard:read";
    public static final String SPORTS_READ = "sports:read";
    public static final String TEAMS_READ = "teams:read";
    public static final String PLAYERS_READ = "players:read";
    public static final String TOURNAMENTS_READ = "tournaments:read";
    public static final String TOURNAMENT_TEAMS_READ = "tournamentTeams:read";
    public static final String TOURNAMENT_STAGES_READ = "tournamentStages:read";
    public static final String STAGE_GROUPS_READ = "stageGroups:read";
    public static final String ROSTERS_READ = "rosters:read";
    public static final String MATCHES_READ = "matches:read";
    public static final String STANDINGS_READ = "standings:read";
    public static final String AUTH_SESSION_READ = "auth:session:read";
    public static final String OPERATIONAL_AUDIT_READ = "operations:audit:read";
    public static final String PERMISSION_GOVERNANCE_MANAGE = "permissions:govern:manage";
    public static final String USERS_READ = "users:read";
    public static final String USERS_MANAGE = "users:manage";
    public static final String BASIC_CONFIGURATION_READ = "configuration:basic:read";
    public static final String BASIC_CONFIGURATION_MANAGE = "configuration:basic:manage";

    public static final String TEAMS_MANAGE = "teams:manage";
    public static final String PLAYERS_MANAGE = "players:manage";
    public static final String TOURNAMENTS_MANAGE = "tournaments:manage";
    public static final String TOURNAMENT_TEAMS_MANAGE = "tournamentTeams:manage";
    public static final String TOURNAMENT_STAGES_MANAGE = "tournamentStages:manage";
    public static final String STAGE_GROUPS_MANAGE = "stageGroups:manage";
    public static final String ROSTERS_MANAGE = "rosters:manage";
    public static final String MATCHES_MANAGE = "matches:manage";
    public static final String STANDINGS_MANAGE = "standings:manage";

    public static final String TEAMS_DELETE = "teams:delete";
    public static final String PLAYERS_DELETE = "players:delete";
    public static final String TOURNAMENTS_DELETE = "tournaments:delete";
    public static final String TOURNAMENT_TEAMS_DELETE = "tournamentTeams:delete";
    public static final String TOURNAMENT_STAGES_DELETE = "tournamentStages:delete";
    public static final String STAGE_GROUPS_DELETE = "stageGroups:delete";
    public static final String ROSTERS_DELETE = "rosters:delete";
    public static final String MATCHES_DELETE = "matches:delete";
    public static final String STANDINGS_DELETE = "standings:delete";

    public static final String TOURNAMENTS_STATUS_TRANSITION = "tournaments:status-transition";
    public static final String TOURNAMENTS_PROGRESS_TO_KNOCKOUT = "tournaments:progress-to-knockout";
    public static final String TOURNAMENTS_GENERATE_KNOCKOUT_BRACKET = "tournaments:generate-knockout-bracket";
    public static final String STANDINGS_RECALCULATE = "standings:recalculate";

    public static final String CAN_READ_AUTH_SESSION = "hasAuthority('" + AUTH_SESSION_READ + "')";
    public static final String CAN_READ_OPERATIONAL_AUDIT = "hasAuthority('" + OPERATIONAL_AUDIT_READ + "')";
    public static final String CAN_MANAGE_PERMISSION_GOVERNANCE = "hasAuthority('" + PERMISSION_GOVERNANCE_MANAGE + "')";
    public static final String CAN_READ_USERS = "hasAuthority('" + USERS_READ + "')";
    public static final String CAN_MANAGE_USERS = "hasAuthority('" + USERS_MANAGE + "')";
    public static final String CAN_READ_BASIC_CONFIGURATION = "hasAuthority('" + BASIC_CONFIGURATION_READ + "')";
    public static final String CAN_MANAGE_BASIC_CONFIGURATION = "hasAuthority('" + BASIC_CONFIGURATION_MANAGE + "')";
    public static final String CAN_MANAGE_TEAMS = "hasAuthority('" + TEAMS_MANAGE + "')";
    public static final String CAN_DELETE_TEAMS = "hasAuthority('" + TEAMS_DELETE + "')";
    public static final String CAN_MANAGE_PLAYERS = "hasAuthority('" + PLAYERS_MANAGE + "')";
    public static final String CAN_DELETE_PLAYERS = "hasAuthority('" + PLAYERS_DELETE + "')";
    public static final String CAN_MANAGE_TOURNAMENTS = "hasAuthority('" + TOURNAMENTS_MANAGE + "')";
    public static final String CAN_DELETE_TOURNAMENTS = "hasAuthority('" + TOURNAMENTS_DELETE + "')";
    public static final String CAN_TRANSITION_TOURNAMENT_STATUS = "hasAuthority('" + TOURNAMENTS_STATUS_TRANSITION + "')";
    public static final String CAN_PROGRESS_TOURNAMENT_TO_KNOCKOUT = "hasAuthority('" + TOURNAMENTS_PROGRESS_TO_KNOCKOUT + "')";
    public static final String CAN_GENERATE_TOURNAMENT_KNOCKOUT_BRACKET = "hasAuthority('" + TOURNAMENTS_GENERATE_KNOCKOUT_BRACKET + "')";
    public static final String CAN_MANAGE_TOURNAMENT_TEAMS = "hasAuthority('" + TOURNAMENT_TEAMS_MANAGE + "')";
    public static final String CAN_DELETE_TOURNAMENT_TEAMS = "hasAuthority('" + TOURNAMENT_TEAMS_DELETE + "')";
    public static final String CAN_MANAGE_TOURNAMENT_STAGES = "hasAuthority('" + TOURNAMENT_STAGES_MANAGE + "')";
    public static final String CAN_DELETE_TOURNAMENT_STAGES = "hasAuthority('" + TOURNAMENT_STAGES_DELETE + "')";
    public static final String CAN_MANAGE_STAGE_GROUPS = "hasAuthority('" + STAGE_GROUPS_MANAGE + "')";
    public static final String CAN_DELETE_STAGE_GROUPS = "hasAuthority('" + STAGE_GROUPS_DELETE + "')";
    public static final String CAN_MANAGE_ROSTERS = "hasAuthority('" + ROSTERS_MANAGE + "')";
    public static final String CAN_DELETE_ROSTERS = "hasAuthority('" + ROSTERS_DELETE + "')";
    public static final String CAN_MANAGE_MATCHES = "hasAuthority('" + MATCHES_MANAGE + "')";
    public static final String CAN_DELETE_MATCHES = "hasAuthority('" + MATCHES_DELETE + "')";
    public static final String CAN_MANAGE_STANDINGS = "hasAuthority('" + STANDINGS_MANAGE + "')";
    public static final String CAN_DELETE_STANDINGS = "hasAuthority('" + STANDINGS_DELETE + "')";
    public static final String CAN_RECALCULATE_STANDINGS = "hasAuthority('" + STANDINGS_RECALCULATE + "')";
}
