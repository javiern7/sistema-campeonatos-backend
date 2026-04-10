package com.multideporte.backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.multideporte.backend.match.controller.MatchGameController;
import com.multideporte.backend.match.dto.response.MatchGameResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.service.MatchGameService;
import com.multideporte.backend.security.audit.OperationalAuditController;
import com.multideporte.backend.security.audit.OperationalAuditResult;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.audit.dto.OperationalActivitySummaryResponse;
import com.multideporte.backend.security.audit.dto.OperationalAuditEventResponse;
import com.multideporte.backend.security.auth.AuthSessionController;
import com.multideporte.backend.security.auth.AuthTokenResponse;
import com.multideporte.backend.security.auth.AuthTokenService;
import com.multideporte.backend.security.auth.AuthenticatedTokenSession;
import com.multideporte.backend.security.auth.AuthorizationCapabilityService;
import com.multideporte.backend.security.auth.PermissionResolutionDiagnosticsService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.auth.dto.PermissionResolutionSummaryResponse;
import com.multideporte.backend.security.auth.dto.RolePermissionResolutionResponse;
import com.multideporte.backend.security.config.SecurityConfig;
import com.multideporte.backend.security.governance.PermissionGovernanceController;
import com.multideporte.backend.security.governance.PermissionGovernanceService;
import com.multideporte.backend.security.governance.dto.ManagedPermissionResponse;
import com.multideporte.backend.security.governance.dto.ManagedRolePermissionResponse;
import com.multideporte.backend.security.governance.dto.PermissionGovernanceSummaryResponse;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.AuthenticatedUser;
import com.multideporte.backend.security.user.DatabaseUserDetailsService;
import com.multideporte.backend.sport.controller.SportController;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.service.SportService;
import com.multideporte.backend.team.controller.TeamController;
import com.multideporte.backend.team.service.TeamService;
import com.multideporte.backend.tournament.controller.TournamentController;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.service.TournamentService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        SportController.class,
        TeamController.class,
        MatchGameController.class,
        TournamentController.class,
        AuthSessionController.class,
        OperationalAuditController.class,
        PermissionGovernanceController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class SecurityContractWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SportService sportService;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private MatchGameService matchGameService;

    @MockitoBean
    private TournamentService tournamentService;

    @MockitoBean
    private OperationalAuditService operationalAuditService;

    @MockitoBean
    private PermissionResolutionDiagnosticsService permissionResolutionDiagnosticsService;

    @MockitoBean
    private PermissionGovernanceService permissionGovernanceService;


    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AuthorizationCapabilityService authorizationCapabilityService;

    @MockitoBean
    private AuthTokenService authTokenService;

    @Test
    void shouldReturnJsonUnauthorizedResponseWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/sports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"));
    }

    @Test
    void shouldRejectBasicCredentialsAfterClosingTransition() throws Exception {
        mockMvc.perform(get("/sports")
                        .header("Authorization", "Basic ZGV2YWRtaW46YWRtaW4xMjM="))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldLoginWithFormalBearerSession() throws Exception {
        when(authTokenService.login(
                org.mockito.ArgumentMatchers.eq("devadmin"),
                org.mockito.ArgumentMatchers.eq("admin123"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new AuthTokenResponse(
                "Bearer",
                "BEARER",
                44L,
                "access-token-123",
                OffsetDateTime.parse("2026-04-05T10:15:30Z"),
                "refresh-token-123",
                OffsetDateTime.parse("2026-04-12T10:15:30Z")
        ));

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "devadmin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.data.authenticationScheme").value("BEARER"))
                .andExpect(jsonPath("$.data.sessionId").value(44))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-123"));
    }

    @Test
    void shouldRefreshFormalBearerSession() throws Exception {
        when(authTokenService.refresh("refresh-token-123")).thenReturn(
                new AuthTokenResponse(
                        "Bearer",
                        "BEARER",
                        44L,
                        "access-token-456",
                        OffsetDateTime.parse("2026-04-05T12:15:30Z"),
                        "refresh-token-456",
                        OffsetDateTime.parse("2026-04-12T12:15:30Z")
                )
        );

        mockMvc.perform(post("/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {
                                  "refreshToken": "refresh-token-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-456"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-456"));
    }

    @Test
    void shouldAuthenticateSportsReadUsingBearerToken() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                22L,
                "bearer-user",
                "",
                List.of(new SimpleGrantedAuthority(SecurityPermissions.SPORTS_READ))
        );
        when(authTokenService.authenticateAccessToken("access-token-123")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        88L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));
        when(sportService.getAll(true)).thenReturn(List.of(
                new SportResponse(1L, "FOOTBALL", "Football", true)
        ));

        mockMvc.perform(get("/sports")
                        .header("Authorization", "Bearer access-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("FOOTBALL"));
    }

    @Test
    void shouldDenyAuthenticatedUserWithoutSportsReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                23L,
                "limited-user",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        when(authTokenService.authenticateAccessToken("limited-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        89L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));

        mockMvc.perform(get("/sports")
                        .header("Authorization", "Bearer limited-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAllowOperatorToManageMatches() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                24L,
                "operator",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_OPERATOR"),
                        new SimpleGrantedAuthority(SecurityPermissions.MATCHES_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("operator-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        90L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));
        when(matchGameService.create(org.mockito.ArgumentMatchers.any())).thenReturn(
                new MatchGameResponse(
                        5L,
                        10L,
                        null,
                        null,
                        1,
                        1,
                        20L,
                        21L,
                        null,
                        "Cancha 1",
                        MatchGameStatus.SCHEDULED,
                        null,
                        null,
                        null,
                        null,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )
        );

        mockMvc.perform(post("/matches")
                        .header("Authorization", "Bearer operator-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "tournamentId": 10,
                                  "roundNumber": 1,
                                  "matchdayNumber": 1,
                                  "homeTournamentTeamId": 20,
                                  "awayTournamentTeamId": 21,
                                  "status": "SCHEDULED"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MATCH_CREATED"))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void shouldDenyOperatorWhenTryingToManageTeams() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                24L,
                "operator",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_OPERATOR"),
                        new SimpleGrantedAuthority(SecurityPermissions.MATCHES_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("operator-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        90L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));

        mockMvc.perform(post("/teams")
                        .header("Authorization", "Bearer operator-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Equipo Operador",
                                  "shortName": "EO",
                                  "code": "EO",
                                  "active": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldDenyTournamentAdminDeleteWhenDeletePermissionIsMissing() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                25L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.TOURNAMENTS_MANAGE),
                        new SimpleGrantedAuthority(SecurityPermissions.TOURNAMENTS_STATUS_TRANSITION)
                )
        );
        when(authTokenService.authenticateAccessToken("tadmin-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        91L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));

        mockMvc.perform(delete("/tournaments/{id}", 99L)
                        .header("Authorization", "Bearer tadmin-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldReturnFormalSessionContractForBearerSession() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                12L,
                "operator",
                "",
                List.of(new SimpleGrantedAuthority(SecurityPermissions.AUTH_SESSION_READ))
        );
        when(authTokenService.authenticateAccessToken("operator-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        77L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));

        AppRole operatorRole = new AppRole();
        operatorRole.setId(3L);
        operatorRole.setCode("OPERATOR");
        operatorRole.setName("Operator");

        AppUser operator = new AppUser();
        operator.setId(12L);
        operator.setUsername("operator");
        operator.setEmail("operator@local.test");
        operator.setFirstName("Op");
        operator.setLastName("Erator");
        operator.setStatus("ACTIVE");
        operator.setRoles(Set.of(operatorRole));

        when(appUserRepository.findByUsername("operator")).thenReturn(Optional.of(operator));
        when(authorizationCapabilityService.resolvePermissions(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(List.of(
                SecurityPermissions.AUTH_SESSION_READ,
                SecurityPermissions.MATCHES_MANAGE,
                SecurityPermissions.ROSTERS_MANAGE,
                SecurityPermissions.STANDINGS_RECALCULATE
        ));

        mockMvc.perform(get("/auth/session")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(12))
                .andExpect(jsonPath("$.data.authenticationScheme").value("BEARER"))
                .andExpect(jsonPath("$.data.sessionStrategy").value("STATELESS"))
                .andExpect(jsonPath("$.data.sessionId").value(77))
                .andExpect(jsonPath("$.data.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.data.permissions[0]").value(SecurityPermissions.AUTH_SESSION_READ));
    }

    @Test
    void shouldAllowTournamentAdminToTransitionTournamentStatusWithExplicitPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                25L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.TOURNAMENTS_STATUS_TRANSITION)
                )
        );
        when(authTokenService.authenticateAccessToken("tadmin-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        91L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));
        when(tournamentService.transitionStatus(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TournamentResponse(
                        10L,
                        1L,
                        "Torneo Demo",
                        "torneo-demo",
                        "2026",
                        TournamentFormat.LEAGUE,
                        TournamentStatus.OPEN,
                        TournamentOperationalCategory.PRODUCTION,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        16,
                        3,
                        1,
                        0,
                        1L,
                        null,
                        null
                ));

        mockMvc.perform(post("/tournaments/{id}/status-transition", 10L)
                        .header("Authorization", "Bearer tadmin-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetStatus": "OPEN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOURNAMENT_STATUS_CHANGED"))
                .andExpect(jsonPath("$.data.id").value(10));
    }
    @Test
    void shouldAllowReadingOperationalAuditEventsWithExplicitPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                26L,
                "auditor",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.OPERATIONAL_AUDIT_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("audit-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        92L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));
        when(operationalAuditService.getRecentAuditEvents(5)).thenReturn(List.of(
                new OperationalAuditEventResponse(
                        1L,
                        26L,
                        "auditor",
                        "TOURNAMENT_STATUS_TRANSITION",
                        "TOURNAMENT",
                        "10",
                        OffsetDateTime.parse("2026-04-05T10:16:00Z"),
                        OperationalAuditResult.SUCCESS,
                        java.util.Map.of("requestPath", "/operations/audit-events/recent")
                )
        ));

        mockMvc.perform(get("/operations/audit-events/recent")
                        .param("limit", "5")
                        .header("Authorization", "Bearer audit-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_AUDIT_EVENT_RECENT"))
                .andExpect(jsonPath("$.data[0].action").value("TOURNAMENT_STATUS_TRANSITION"))
                .andExpect(jsonPath("$.data[0].result").value("SUCCESS"));
    }

    @Test
    void shouldDenyOperationalAuditReadWithoutPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                27L,
                "operator",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_OPERATOR"),
                        new SimpleGrantedAuthority(SecurityPermissions.MATCHES_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("operator-no-audit-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        93L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));

        mockMvc.perform(get("/operations/audit-events/recent")
                        .header("Authorization", "Bearer operator-no-audit-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    @Test
    void shouldExposePermissionResolutionSummaryForAdministrativeAuditUsers() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                28L,
                "auditor",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.OPERATIONAL_AUDIT_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("audit-summary-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        94L,
                        OffsetDateTime.parse("2026-04-05T10:15:30Z")
                )
        ));
        when(permissionResolutionDiagnosticsService.getSummary()).thenReturn(
                new PermissionResolutionSummaryResponse(
                        OffsetDateTime.parse("2026-04-07T12:00:00Z"),
                        true,
                        1,
                        List.of("LEGACY_ROLE"),
                        List.of(
                                new RolePermissionResolutionResponse(
                                        "OPERATOR",
                                        4,
                                        4,
                                        false,
                                        List.of(
                                                SecurityPermissions.AUTH_SESSION_READ,
                                                SecurityPermissions.DASHBOARD_READ,
                                                SecurityPermissions.MATCHES_MANAGE,
                                                SecurityPermissions.ROSTERS_MANAGE
                                        ),
                                        List.of(
                                                SecurityPermissions.AUTH_SESSION_READ,
                                                SecurityPermissions.DASHBOARD_READ,
                                                SecurityPermissions.MATCHES_MANAGE,
                                                SecurityPermissions.ROSTERS_MANAGE
                                        )
                                ),
                                new RolePermissionResolutionResponse(
                                        "LEGACY_ROLE",
                                        0,
                                        1,
                                        true,
                                        List.of(),
                                        List.of(SecurityPermissions.DASHBOARD_READ)
                                )
                        )
                )
        );

        mockMvc.perform(get("/operations/permission-resolution-summary")
                        .header("Authorization", "Bearer audit-summary-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PERMISSION_RESOLUTION_SUMMARY"))
                .andExpect(jsonPath("$.data.fallbackActive").value(true))
                .andExpect(jsonPath("$.data.rolesUsingFallback[0]").value("LEGACY_ROLE"))
                .andExpect(jsonPath("$.data.roles[0].roleCode").value("OPERATOR"));
    }

    @Test
    void shouldExposePermissionGovernanceSummaryForAuditUsers() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                29L,
                "auditor",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.OPERATIONAL_AUDIT_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("governance-summary-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        95L,
                        OffsetDateTime.parse("2026-04-08T10:15:30Z")
                )
        ));
        when(permissionGovernanceService.getSummary()).thenReturn(
                new PermissionGovernanceSummaryResponse(
                        OffsetDateTime.parse("2026-04-08T11:00:00Z"),
                        false,
                        List.of("TOURNAMENT_ADMIN", "OPERATOR"),
                        List.of(new ManagedPermissionResponse(
                                SecurityPermissions.MATCHES_MANAGE,
                                "Matches manage",
                                "Permite crear y actualizar partidos"
                        )),
                        List.of(new ManagedRolePermissionResponse(
                                "OPERATOR",
                                "Operator",
                                true,
                                List.of(SecurityPermissions.MATCHES_MANAGE)
                        ))
                )
        );

        mockMvc.perform(get("/operations/permission-governance/roles")
                        .header("Authorization", "Bearer governance-summary-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PERMISSION_GOVERNANCE_SUMMARY"))
                .andExpect(jsonPath("$.data.writeEnabled").value(false))
                .andExpect(jsonPath("$.data.mutableRoles[0]").value("TOURNAMENT_ADMIN"))
                .andExpect(jsonPath("$.data.roles[0].roleCode").value("OPERATOR"));
    }

    @Test
    void shouldAllowSuperAdminToUpdateRolePermissionsWithExplicitGovernancePermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                30L,
                "superadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.PERMISSION_GOVERNANCE_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("governance-manage-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        96L,
                        OffsetDateTime.parse("2026-04-08T10:15:30Z")
                )
        ));
        when(permissionGovernanceService.replaceRolePermissions(
                org.mockito.ArgumentMatchers.eq("OPERATOR"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new ManagedRolePermissionResponse(
                "OPERATOR",
                "Operator",
                true,
                List.of(
                        SecurityPermissions.AUTH_SESSION_READ,
                        SecurityPermissions.MATCHES_MANAGE
                )
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/permission-governance/roles/{roleCode}", "OPERATOR")
                        .header("Authorization", "Bearer governance-manage-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"permissionCodes\": [
                                    \"auth:session:read\",
                                    \"matches:manage\"
                                  ],
                                  \"reason\": \"ajuste operativo controlado\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PERMISSION_ROLE_ASSIGNMENTS_UPDATED"))
                .andExpect(jsonPath("$.data.roleCode").value("OPERATOR"))
                .andExpect(jsonPath("$.data.permissionCodes[1]").value(SecurityPermissions.MATCHES_MANAGE));
    }

    @Test
    void shouldDenyRolePermissionUpdateWithoutGovernancePermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                31L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.OPERATIONAL_AUDIT_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("no-governance-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        97L,
                        OffsetDateTime.parse("2026-04-08T10:15:30Z")
                )
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/permission-governance/roles/{roleCode}", "OPERATOR")
                        .header("Authorization", "Bearer no-governance-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"permissionCodes\": [
                                    \"matches:manage\"
                                  ],
                                  \"reason\": \"ajuste operativo controlado\"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}








