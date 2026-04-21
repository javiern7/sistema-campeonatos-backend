package com.multideporte.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AuthenticatedPostgreSqlIntegrationTestSupport extends PostgreSqlContainerConfig {

    protected static final String ADMIN_USERNAME = "devadmin";
    protected static final String PASSWORD = "admin123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private final Map<String, String> accessTokens = new ConcurrentHashMap<>();

    protected String adminAccessToken() throws Exception {
        return accessTokenFor(ADMIN_USERNAME, PASSWORD);
    }

    protected String accessTokenFor(String username, String password) throws Exception {
        String cacheKey = username + ":" + password;
        String cachedToken = accessTokens.get(cacheKey);
        if (cachedToken != null) {
            return cachedToken;
        }

        String accessToken = loginAndExtractAccessToken(username, password);
        accessTokens.put(cacheKey, accessToken);
        return accessToken;
    }

    protected String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String loginAndExtractAccessToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
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
}

