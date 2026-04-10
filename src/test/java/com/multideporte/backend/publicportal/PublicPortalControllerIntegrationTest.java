package com.multideporte.backend.publicportal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.support.PostgreSqlContainerConfig;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PublicPortalControllerIntegrationTest extends PostgreSqlContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeOnlyProductionTournamentsInPublicPortal() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CreatedTournament visibleTournament = createTournament("Portal Visible " + suffix, "2026-" + suffix, null);
        transitionTournament(visibleTournament.id(), TournamentStatus.OPEN);

        CreatedTournament hiddenTournament = createTournament(
                "Portal Interno " + suffix,
                "INT-" + suffix,
                TournamentOperationalCategory.QA
        );

        mockMvc.perform(get("/api/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_PORTAL_HOME_FOUND"))
                .andExpect(jsonPath("$.data.visibleTournamentCount").value(1));

        mockMvc.perform(get("/api/public/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Portal Visible " + suffix));

        mockMvc.perform(get("/api/public/tournaments/{slug}", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules.resultsEnabled").value(true))
                .andExpect(jsonPath("$.data.modules.approvedPiecesEnabled").value(false));

        mockMvc.perform(get("/api/public/tournaments/{slug}/standings", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEntries").value(0));

        mockMvc.perform(get("/api/public/tournaments/{slug}/results", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClosedMatches").value(0));

        mockMvc.perform(get("/api/public/tournaments/{slug}", hiddenTournament.slug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private CreatedTournament createTournament(String name, String seasonName, TournamentOperationalCategory operationalCategory) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
                operationalCategory,
                "Contrato portal publico minimo",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                16,
                3,
                1,
                0
        );

        JsonNode root = objectMapper.readTree(mockMvc.perform(post("/api/tournaments")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        return new CreatedTournament(root.path("data").path("id").asLong(), root.path("data").path("slug").asText());
    }

    private void transitionTournament(long tournamentId, TournamentStatus status) throws Exception {
        mockMvc.perform(post("/api/tournaments/{id}/status-transition", tournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", status.name()))))
                .andExpect(status().isOk());
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private record CreatedTournament(long id, String slug) {
    }
}
