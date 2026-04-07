package com.multideporte.backend.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.support.PostgreSqlContainerConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthSessionAuthorizationIntegrationTest extends PostgreSqlContainerConfig {

    private static final String ADMIN_USERNAME = "devadmin";
    private static final String VALIDATION_USERNAME = "devoperator";
    private static final String PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAllowOperationalAuditEndpointsForAdminSession() throws Exception {
        String accessToken = loginAndExtractAccessToken(ADMIN_USERNAME, PASSWORD);

        mockMvc.perform(get("/api/operations/audit-events/recent")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_AUDIT_EVENT_RECENT"))
                .andExpect(jsonPath("$.data[0].action").exists());

        mockMvc.perform(get("/api/operations/activity-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OPERATIONAL_ACTIVITY_SUMMARY"))
                .andExpect(jsonPath("$.data.totalEvents").isNumber());
    }

    @Test
    void shouldDenyOperationalAuditEndpointsForValidationOperator() throws Exception {
        String accessToken = loginAndExtractAccessToken(VALIDATION_USERNAME, PASSWORD);

        mockMvc.perform(get("/api/operations/audit-events/recent")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/operations/activity-summary")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldExposeEffectiveSessionPermissionsWithoutOperationalAuditForValidationOperator() throws Exception {
        String accessToken = loginAndExtractAccessToken(VALIDATION_USERNAME, PASSWORD);

        mockMvc.perform(get("/api/auth/session")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_SESSION"))
                .andExpect(jsonPath("$.data.username").value(VALIDATION_USERNAME))
                .andExpect(jsonPath("$.data.roles", hasItem("OPERATOR")))
                .andExpect(jsonPath("$.data.permissions", hasItem(SecurityPermissions.DASHBOARD_READ)))
                .andExpect(jsonPath("$.data.permissions", hasItem(SecurityPermissions.AUTH_SESSION_READ)))
                .andExpect(jsonPath("$.data.permissions", not(hasItem(SecurityPermissions.OPERATIONAL_AUDIT_READ))));
    }

    private String loginAndExtractAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AUTH_LOGIN_SUCCESS"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
