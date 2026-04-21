package com.multideporte.backend.publicportal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.multideporte.backend.publicportal.controller.PublicPortalController;
import com.multideporte.backend.publicportal.dto.PublicPortalHomeResponse;
import com.multideporte.backend.publicportal.dto.PublicReadModulesResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentCalendarResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentDetailResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentResultsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentStandingsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentSummaryResponse;
import com.multideporte.backend.publicportal.service.PublicPortalService;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.AuthTokenService;
import com.multideporte.backend.security.config.SecurityConfig;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.DatabaseUserDetailsService;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicPortalController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class PublicPortalSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PublicPortalService publicPortalService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private OperationalAuditService operationalAuditService;

    @Test
    void shouldExposePublicHomeWithoutAuthentication() throws Exception {
        when(publicPortalService.getHome()).thenReturn(new PublicPortalHomeResponse(
                "Sistema Campeonatos",
                OffsetDateTime.parse("2026-04-10T12:00:00Z"),
                1,
                1,
                0,
                0,
                List.of(new PublicTournamentSummaryResponse(
                        10L,
                        1L,
                        "Futbol",
                        "Copa Apertura",
                        "copa-apertura-2026",
                        "2026",
                        TournamentFormat.LEAGUE,
                        TournamentStatus.IN_PROGRESS,
                        "Portal publico",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 6, 30)
                )),
                new PublicReadModulesResponse(true, true, true, false)
        ));

        mockMvc.perform(get("/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_PORTAL_HOME_FOUND"))
                .andExpect(jsonPath("$.data.featuredTournaments[0].slug").value("copa-apertura-2026"));
    }

    @Test
    void shouldExposePublicTournamentReadsWithoutAuthentication() throws Exception {
        when(publicPortalService.getTournaments(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()
        ))
                .thenReturn(new PageImpl<>(List.of(new PublicTournamentSummaryResponse(
                        10L,
                        1L,
                        "Futbol",
                        "Copa Apertura",
                        "copa-apertura-2026",
                        "2026",
                        TournamentFormat.LEAGUE,
                        TournamentStatus.OPEN,
                        "Portal publico",
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 6, 30)
                ))));
        when(publicPortalService.getTournamentDetail("copa-apertura-2026")).thenReturn(new PublicTournamentDetailResponse(
                10L,
                1L,
                "Futbol",
                "Copa Apertura",
                "copa-apertura-2026",
                "2026",
                TournamentFormat.LEAGUE,
                TournamentStatus.OPEN,
                "Portal publico",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-04-10T12:00:00Z"),
                new PublicReadModulesResponse(true, true, true, false)
        ));
        when(publicPortalService.getTournamentStandings("copa-apertura-2026", null, null)).thenReturn(
                new PublicTournamentStandingsResponse(10L, "copa-apertura-2026", null, null, null, null, null, null, 0, List.of())
        );
        when(publicPortalService.getTournamentCalendar("copa-apertura-2026", null, null, null, null, null)).thenReturn(
                new PublicTournamentCalendarResponse(10L, "copa-apertura-2026", null, null, null, null, null, 0, 0, 0, List.of())
        );
        when(publicPortalService.getTournamentResults("copa-apertura-2026", null, null)).thenReturn(
                new PublicTournamentResultsResponse(10L, "copa-apertura-2026", null, null, 0, List.of())
        );

        mockMvc.perform(get("/public/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_TOURNAMENT_PAGE"))
                .andExpect(jsonPath("$.data.content[0].name").value("Copa Apertura"));

        mockMvc.perform(get("/public/tournaments/copa-apertura-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules.approvedPiecesEnabled").value(false));

        mockMvc.perform(get("/public/tournaments/copa-apertura-2026/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_TOURNAMENT_STANDINGS_FOUND"));

        mockMvc.perform(get("/public/tournaments/copa-apertura-2026/calendar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_TOURNAMENT_CALENDAR_FOUND"));

        mockMvc.perform(get("/public/tournaments/copa-apertura-2026/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_TOURNAMENT_RESULTS_FOUND"));
    }

    @Test
    void shouldKeepOperationalEndpointsProtectedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/operations/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
