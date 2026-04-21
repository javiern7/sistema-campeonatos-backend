package com.multideporte.backend.reporting;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.multideporte.backend.reporting.controller.OperationalReportingController;
import com.multideporte.backend.reporting.dto.MatchReportRow;
import com.multideporte.backend.reporting.dto.OperationalReportResponse;
import com.multideporte.backend.reporting.dto.ReportExportResponse;
import com.multideporte.backend.reporting.dto.ReportFiltersResponse;
import com.multideporte.backend.reporting.dto.ReportMetadataResponse;
import com.multideporte.backend.reporting.dto.ReportTournamentResponse;
import com.multideporte.backend.reporting.dto.TournamentSummaryReportRow;
import com.multideporte.backend.reporting.service.OperationalReportingService;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.AuthTokenService;
import com.multideporte.backend.security.auth.AuthenticatedTokenSession;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.config.SecurityConfig;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.AuthenticatedUser;
import com.multideporte.backend.security.user.DatabaseUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OperationalReportingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
class OperationalReportingWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationalReportingService operationalReportingService;

    @MockitoBean
    private DatabaseUserDetailsService databaseUserDetailsService;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @MockitoBean
    private AuthTokenService authTokenService;

    @MockitoBean
    private OperationalAuditService operationalAuditService;

    @Test
    void shouldExposeTournamentSummaryReportForReadPermission() throws Exception {
        when(authTokenService.authenticateAccessToken("report-token")).thenReturn(Optional.of(authenticatedSession(
                SecurityPermissions.TOURNAMENTS_READ,
                SecurityPermissions.MATCHES_READ,
                SecurityPermissions.STANDINGS_READ
        )));
        when(operationalReportingService.getTournamentSummary(7L)).thenReturn(new OperationalReportResponse<>(
                new ReportMetadataResponse("tournament-summary", "json", Instant.parse("2026-04-20T15:00:00Z"), "standing", List.of("Read-only")),
                new ReportTournamentResponse(7L, "Copa Demo", "2026", "LEAGUE", "OPEN", "PRODUCTION"),
                new ReportFiltersResponse(7L, null, null, null, null, null, null),
                java.util.Map.of("rows", 1),
                List.of(new TournamentSummaryReportRow(7L, "Copa Demo", 8L, 12L, 8L, 4, 20, 3, 1))
        ));

        mockMvc.perform(get("/tournaments/{tournamentId}/reports/summary", 7L)
                        .header("Authorization", "Bearer report-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_TOURNAMENT_SUMMARY_FOUND"))
                .andExpect(jsonPath("$.data.rows[0].tournamentId").value(7))
                .andExpect(jsonPath("$.data.rows[0].matches").value(12));
    }

    @Test
    void shouldExposeMatchesReportForMatchesReadPermission() throws Exception {
        when(authTokenService.authenticateAccessToken("report-token")).thenReturn(Optional.of(authenticatedSession(
                SecurityPermissions.MATCHES_READ
        )));
        when(operationalReportingService.getMatchesReport(7L, null, null, null, null, null)).thenReturn(new OperationalReportResponse<>(
                new ReportMetadataResponse("matches", "json", Instant.parse("2026-04-20T15:10:00Z"), "match_game", List.of("Read-only")),
                new ReportTournamentResponse(7L, "Copa Demo", "2026", "LEAGUE", "OPEN", "PRODUCTION"),
                new ReportFiltersResponse(7L, null, null, null, null, null, null),
                java.util.Map.of("rows", 1),
                List.of(new MatchReportRow(44L, 1L, null, 1, 1, 20L, "Alpha FC", 21L, "Beta FC", OffsetDateTime.parse("2026-04-20T18:00:00Z"), "Cancha 1", "SCHEDULED", null, null, null))
        ));

        mockMvc.perform(get("/tournaments/{tournamentId}/reports/matches", 7L)
                        .header("Authorization", "Bearer report-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REPORT_MATCHES_FOUND"))
                .andExpect(jsonPath("$.data.rows[0].matchId").value(44))
                .andExpect(jsonPath("$.data.rows[0].homeTeamName").value("Alpha FC"));
    }

    @Test
    void shouldExportTournamentSummaryAsCsv() throws Exception {
        when(authTokenService.authenticateAccessToken("report-token")).thenReturn(Optional.of(authenticatedSession(
                SecurityPermissions.TOURNAMENTS_READ,
                SecurityPermissions.MATCHES_READ,
                SecurityPermissions.STANDINGS_READ
        )));
        when(operationalReportingService.exportFile(7L, "summary", "csv", null, null, null, null, null, null, null)).thenReturn(
                new ReportExportResponse(
                        "tournament-7-tournament-summary.csv",
                        "text/csv; charset=UTF-8",
                        "header1,header2\nvalue1,value2\n".getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(get("/tournaments/{tournamentId}/reports/export", 7L)
                        .param("reportType", "summary")
                        .param("format", "csv")
                        .header("Authorization", "Bearer report-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("tournament-7-tournament-summary.csv")))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")));
    }

    private AuthenticatedTokenSession authenticatedSession(String... permissions) {
        List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        for (String permission : permissions) {
            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(permission));
        }
        return new AuthenticatedTokenSession(
                new AuthenticatedUser(55L, "superadmin", "", authorities),
                205L,
                OffsetDateTime.parse("2026-04-20T20:00:00Z")
        );
    }
}
