package com.multideporte.backend.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.multideporte.backend.security.config.SecurityConfig;
import com.multideporte.backend.security.user.DatabaseUserDetailsService;
import com.multideporte.backend.sport.controller.SportController;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.service.SportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SportController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class SecurityContractWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SportService sportService;

    @MockBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @Test
    void shouldReturnJsonUnauthorizedResponseWhenCredentialsAreMissing() throws Exception {
        mockMvc.perform(get("/sports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"));
    }

    @Test
    void shouldReturnSportsWhenCredentialsAreValid() throws Exception {
        when(sportService.getAll(true)).thenReturn(List.of(
                new SportResponse(1L, "FOOTBALL", "Football", true)
        ));
        when(databaseUserDetailsService.loadUserByUsername("devadmin")).thenReturn(
                new User(
                        "devadmin",
                        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin123"),
                        List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );

        mockMvc.perform(get("/sports")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("FOOTBALL"));
    }
}
