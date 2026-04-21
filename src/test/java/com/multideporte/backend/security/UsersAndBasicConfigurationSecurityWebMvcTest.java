package com.multideporte.backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.AuthTokenService;
import com.multideporte.backend.security.auth.AuthenticatedTokenSession;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.basicconfiguration.BasicConfigurationController;
import com.multideporte.backend.security.basicconfiguration.BasicConfigurationService;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationResponse;
import com.multideporte.backend.security.config.SecurityConfig;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.AuthenticatedUser;
import com.multideporte.backend.security.user.DatabaseUserDetailsService;
import com.multideporte.backend.security.usermanagement.OperationalAdministrationCatalogController;
import com.multideporte.backend.security.usermanagement.OperationalUserManagementController;
import com.multideporte.backend.security.usermanagement.OperationalUserManagementService;
import com.multideporte.backend.security.usermanagement.dto.OperationalPermissionResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalRoleResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserDetailResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserPermissionSummaryResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserResponse;
import com.multideporte.backend.security.usermanagement.dto.OperationalUserStatusResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        OperationalUserManagementController.class,
        OperationalAdministrationCatalogController.class,
        BasicConfigurationController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class UsersAndBasicConfigurationSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationalUserManagementService operationalUserManagementService;

    @MockitoBean
    private BasicConfigurationService basicConfigurationService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private OperationalAuditService operationalAuditService;

    @Test
    void shouldExposeOperationalUsersForUsersReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                32L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.USERS_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.getUsers(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                new OperationalUserResponse(
                        15L,
                        "operator",
                        "operator@local.test",
                        "Op",
                        "Erator",
                        "Op Erator",
                        "ACTIVE",
                        OffsetDateTime.parse("2026-04-09T09:00:00Z"),
                        List.of("OPERATOR"),
                        true
                )
        )));

        mockMvc.perform(get("/operations/users")
                        .header("Authorization", "Bearer users-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USERS_PAGE"))
                .andExpect(jsonPath("$.data.content[0].username").value("operator"));
    }

    @Test
    void shouldAllowSuperAdminToUpdateOperationalUserStatus() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                33L,
                "superadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.USERS_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("users-manage-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        99L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.updateStatus(
                org.mockito.ArgumentMatchers.eq(15L),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new OperationalUserResponse(
                15L,
                "operator",
                "operator@local.test",
                "Op",
                "Erator",
                "Op Erator",
                "LOCKED",
                OffsetDateTime.parse("2026-04-09T09:00:00Z"),
                List.of("OPERATOR"),
                true
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/users/{id}/status", 15L)
                        .header("Authorization", "Bearer users-manage-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"status\": \"LOCKED\",
                                  \"reason\": \"bloqueo operativo\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USER_STATUS_UPDATED"))
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void shouldExposeOperationalUserDetailForUsersReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = usersReadUser();
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.getUserDetail(15L)).thenReturn(new OperationalUserDetailResponse(
                15L,
                "operator",
                "operator@local.test",
                "Op",
                "Erator",
                "Op Erator",
                "ACTIVE",
                OffsetDateTime.parse("2026-04-09T09:00:00Z"),
                List.of(new OperationalRoleResponse("OPERATOR", "Operator", "Opera resultados", true, true, null)),
                true,
                null,
                true,
                null
        ));

        mockMvc.perform(get("/operations/users/{id}", 15L)
                        .header("Authorization", "Bearer users-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USER_DETAIL"))
                .andExpect(jsonPath("$.data.roles[0].roleCode").value("OPERATOR"));
    }

    @Test
    void shouldExposeOperationalUserPermissionsForUsersReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = usersReadUser();
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.getUserPermissions(15L)).thenReturn(
                new OperationalUserPermissionSummaryResponse(
                        15L,
                        "operator",
                        List.of(new OperationalRoleResponse("OPERATOR", "Operator", "Opera resultados", true, true, null)),
                        List.of(new OperationalPermissionResponse(SecurityPermissions.MATCHES_MANAGE, "Matches manage", "Permite gestionar partidos"))
                )
        );

        mockMvc.perform(get("/operations/users/{id}/permissions", 15L)
                        .header("Authorization", "Bearer users-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USER_PERMISSIONS"))
                .andExpect(jsonPath("$.data.permissions[0].code").value(SecurityPermissions.MATCHES_MANAGE));
    }

    @Test
    void shouldAllowSuperAdminToUpdateOperationalUserRoles() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                33L,
                "superadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.USERS_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("users-manage-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        99L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.replaceUserRoles(
                org.mockito.ArgumentMatchers.eq(15L),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new OperationalUserDetailResponse(
                15L,
                "operator",
                "operator@local.test",
                "Op",
                "Erator",
                "Op Erator",
                "ACTIVE",
                OffsetDateTime.parse("2026-04-09T09:00:00Z"),
                List.of(new OperationalRoleResponse("TOURNAMENT_ADMIN", "Tournament Admin", "Gestiona torneos", true, true, null)),
                true,
                null,
                true,
                null
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/users/{id}/roles", 15L)
                        .header("Authorization", "Bearer users-manage-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"roleCodes\": [\"TOURNAMENT_ADMIN\"],
                                  \"reason\": \"ajuste operativo\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USER_ROLES_UPDATED"))
                .andExpect(jsonPath("$.data.roles[0].roleCode").value("TOURNAMENT_ADMIN"));
    }

    @Test
    void shouldDenyOperationalUserRolesUpdateWithoutManagePermission() throws Exception {
        AuthenticatedUser authenticatedUser = usersReadUser();
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/users/{id}/roles", 15L)
                        .header("Authorization", "Bearer users-read-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"roleCodes\": [\"OPERATOR\"],
                                  \"reason\": \"ajuste operativo\"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldExposeOperationalRolesForUsersReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = usersReadUser();
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.getRoles()).thenReturn(List.of(
                new OperationalRoleResponse(
                        "OPERATOR",
                        "Operator",
                        "Opera resultados, partidos y rosters",
                        true,
                        true,
                        null
                )
        ));

        mockMvc.perform(get("/operations/roles")
                        .header("Authorization", "Bearer users-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_ROLES"))
                .andExpect(jsonPath("$.data[0].assignable").value(true));
    }

    @Test
    void shouldExposeOperationalUserStatusesForUsersReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = usersReadUser();
        when(authTokenService.authenticateAccessToken("users-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        98L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(operationalUserManagementService.getUserStatuses()).thenReturn(List.of(
                new OperationalUserStatusResponse("ACTIVE", "Activo", "Usuario habilitado")
        ));

        mockMvc.perform(get("/operations/user-statuses")
                        .header("Authorization", "Bearer users-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_USER_STATUSES"))
                .andExpect(jsonPath("$.data[0].code").value("ACTIVE"));
    }
    @Test
    void shouldExposeBasicConfigurationForReadPermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                34L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.BASIC_CONFIGURATION_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("configuration-read-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        100L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(basicConfigurationService.getConfiguration()).thenReturn(new BasicConfigurationResponse(
                "Sistema Campeonatos",
                "operaciones@local.test",
                "America/Lima",
                OffsetDateTime.parse("2026-04-09T10:00:00Z")
        ));

        mockMvc.perform(get("/operations/basic-configuration")
                        .header("Authorization", "Bearer configuration-read-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BASIC_CONFIGURATION_FOUND"))
                .andExpect(jsonPath("$.data.defaultTimezone").value("America/Lima"));
    }

    @Test
    void shouldDenyBasicConfigurationUpdateWithoutManagePermission() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                35L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.BASIC_CONFIGURATION_READ)
                )
        );
        when(authTokenService.authenticateAccessToken("configuration-read-only-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        101L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/basic-configuration")
                        .header("Authorization", "Bearer configuration-read-only-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"organizationName\": \"Sistema Campeonatos\",
                                  \"supportEmail\": \"operaciones@local.test\",
                                  \"defaultTimezone\": \"America/Lima\"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    private AuthenticatedUser usersReadUser() {
        return new AuthenticatedUser(
                32L,
                "tadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_TOURNAMENT_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.USERS_READ)
                )
        );
    }

    @Test
    void shouldAllowSuperAdminToUpdateBasicConfiguration() throws Exception {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                36L,
                "superadmin",
                "",
                List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority(SecurityPermissions.BASIC_CONFIGURATION_MANAGE)
                )
        );
        when(authTokenService.authenticateAccessToken("configuration-manage-token")).thenReturn(Optional.of(
                new AuthenticatedTokenSession(
                        authenticatedUser,
                        104L,
                        OffsetDateTime.parse("2026-04-09T10:15:30Z")
                )
        ));
        when(basicConfigurationService.updateConfiguration(org.mockito.ArgumentMatchers.any())).thenReturn(new BasicConfigurationResponse(
                "Sistema Campeonatos QA",
                "soporte.qa@local.test",
                "America/Bogota",
                OffsetDateTime.parse("2026-04-09T11:00:00Z")
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/operations/basic-configuration")
                        .header("Authorization", "Bearer configuration-manage-token")
                        .contentType("application/json")
                        .content("""
                                {
                                  \"organizationName\": \"Sistema Campeonatos QA\",
                                  \"supportEmail\": \"soporte.qa@local.test\",
                                  \"defaultTimezone\": \"America/Bogota\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BASIC_CONFIGURATION_UPDATED"))
                .andExpect(jsonPath("$.data.organizationName").value("Sistema Campeonatos QA"))
                .andExpect(jsonPath("$.data.defaultTimezone").value("America/Bogota"));
    }
}
