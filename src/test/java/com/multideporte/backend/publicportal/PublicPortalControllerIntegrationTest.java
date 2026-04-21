package com.multideporte.backend.publicportal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.support.PostgreSqlContainerConfig;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

    private String adminAccessToken;

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

        mockMvc.perform(get("/public/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_PORTAL_HOME_FOUND"))
                .andExpect(jsonPath("$.data.visibleTournamentCount").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/public/tournaments")
                        .param("name", "Portal Visible " + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Portal Visible " + suffix));

        mockMvc.perform(get("/public/tournaments/{slug}", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules.resultsEnabled").value(true))
                .andExpect(jsonPath("$.data.modules.approvedPiecesEnabled").value(false));

        mockMvc.perform(get("/public/tournaments/{slug}/standings", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEntries").value(0));

        mockMvc.perform(get("/public/tournaments/{slug}/results", visibleTournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClosedMatches").value(0));

        mockMvc.perform(get("/public/tournaments/{slug}", hiddenTournament.slug()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldExposePublicCalendarWithScheduledMatchesAndKeepResultsClosedOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CreatedTournament tournament = createTournament("Portal Calendar " + suffix, "2026-" + suffix, null);
        transitionTournament(tournament.id(), TournamentStatus.OPEN);

        long teamA = createTeam("PC Team A " + suffix, "PCA" + suffix);
        long teamB = createTeam("PC Team B " + suffix, "PCB" + suffix);
        long tournamentTeamA = createTournamentTeam(tournament.id(), teamA, 1, 1);
        long tournamentTeamB = createTournamentTeam(tournament.id(), teamB, 2, 2);

        long playerA = createPlayer("CalendarA", suffix, "CALA" + suffix);
        long playerB = createPlayer("CalendarB", suffix, "CALB" + suffix);
        createRoster(tournamentTeamA, playerA, 11);
        createRoster(tournamentTeamB, playerB, 12);

        long stageId = createStage(tournament.id(), "Liga Publica " + suffix, "LEAGUE", 1, true);
        transitionTournament(tournament.id(), TournamentStatus.IN_PROGRESS);

        createMatch(
                tournament.id(),
                stageId,
                null,
                1,
                1,
                tournamentTeamA,
                tournamentTeamB,
                MatchGameStatus.PLAYED.name(),
                OffsetDateTime.parse("2026-04-20T15:00:00Z"),
                2,
                1
        );
        createMatch(
                tournament.id(),
                stageId,
                null,
                1,
                2,
                tournamentTeamB,
                tournamentTeamA,
                MatchGameStatus.SCHEDULED.name(),
                OffsetDateTime.parse("2026-04-27T15:00:00Z"),
                null,
                null
        );

        mockMvc.perform(get("/public/tournaments/{slug}/calendar", tournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PUBLIC_TOURNAMENT_CALENDAR_FOUND"))
                .andExpect(jsonPath("$.data.tournamentSlug").value(tournament.slug()))
                .andExpect(jsonPath("$.data.totalMatches").value(2))
                .andExpect(jsonPath("$.data.scheduledMatches").value(1))
                .andExpect(jsonPath("$.data.closedMatches").value(1))
                .andExpect(jsonPath("$.data.matches[0].status").value("PLAYED"))
                .andExpect(jsonPath("$.data.matches[1].status").value("SCHEDULED"));

        mockMvc.perform(get("/public/tournaments/{slug}/calendar", tournament.slug())
                        .param("status", MatchGameStatus.SCHEDULED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMatches").value(1))
                .andExpect(jsonPath("$.data.scheduledMatches").value(1))
                .andExpect(jsonPath("$.data.matches[0].status").value("SCHEDULED"));

        mockMvc.perform(get("/public/tournaments/{slug}/results", tournament.slug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalClosedMatches").value(1))
                .andExpect(jsonPath("$.data.results[0].match.status").value("PLAYED"));
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

        JsonNode root = objectMapper.readTree(mockMvc.perform(post("/tournaments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        return new CreatedTournament(root.path("data").path("id").asLong(), root.path("data").path("slug").asText());
    }

    private void transitionTournament(long tournamentId, TournamentStatus status) throws Exception {
        mockMvc.perform(post("/tournaments/{id}/status-transition", tournamentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", status.name()))))
                .andExpect(status().isOk());
    }

    private long createTeam(String name, String code) throws Exception {
        return extractId(mockMvc.perform(post("/teams")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "shortName", name.substring(0, Math.min(12, name.length())),
                                "code", code,
                                "primaryColor", "#1144AA",
                                "secondaryColor", "#FFFFFF",
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createTournamentTeam(long tournamentId, long teamId, int seedNumber, int groupDrawPosition) throws Exception {
        return extractId(mockMvc.perform(post("/tournament-teams")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "teamId", teamId,
                                "registrationStatus", "APPROVED",
                                "seedNumber", seedNumber,
                                "groupDrawPosition", groupDrawPosition
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createPlayer(String firstName, String lastName, String documentNumber) throws Exception {
        return extractId(mockMvc.perform(post("/players")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", firstName,
                                "lastName", lastName,
                                "documentType", "DNI",
                                "documentNumber", documentNumber,
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createRoster(long tournamentTeamId, long playerId, int jerseyNumber) throws Exception {
        return extractId(mockMvc.perform(post("/rosters")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentTeamId", tournamentTeamId,
                                "playerId", playerId,
                                "jerseyNumber", jerseyNumber,
                                "captain", false,
                                "positionName", "MID",
                                "rosterStatus", RosterStatus.ACTIVE.name(),
                                "startDate", "2026-04-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createStage(long tournamentId, String name, String stageType, int sequenceOrder, boolean active) throws Exception {
        return extractId(mockMvc.perform(post("/tournament-stages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "name", name,
                                "stageType", stageType,
                                "sequenceOrder", sequenceOrder,
                                "legs", 1,
                                "roundTrip", false,
                                "active", active
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createMatch(
            long tournamentId,
            long stageId,
            Long groupId,
            int roundNumber,
            int matchdayNumber,
            long homeTournamentTeamId,
            long awayTournamentTeamId,
            String status,
            OffsetDateTime scheduledAt,
            Integer homeScore,
            Integer awayScore
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tournamentId", tournamentId);
        payload.put("stageId", stageId);
        payload.put("groupId", groupId);
        payload.put("roundNumber", roundNumber);
        payload.put("matchdayNumber", matchdayNumber);
        payload.put("homeTournamentTeamId", homeTournamentTeamId);
        payload.put("awayTournamentTeamId", awayTournamentTeamId);
        payload.put("scheduledAt", scheduledAt);
        payload.put("venueName", "Cancha Publica");
        payload.put("status", status);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        payload.put("winnerTournamentTeamId", homeScore != null && awayScore != null && homeScore > awayScore ? homeTournamentTeamId : null);

        return extractId(mockMvc.perform(post("/matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String adminAccessToken() throws Exception {
        if (adminAccessToken == null) {
            JsonNode root = objectMapper.readTree(mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "username", "devadmin",
                                    "password", "admin123"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("AUTH_LOGIN_SUCCESS"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString());
            adminAccessToken = root.path("data").path("accessToken").asText();
        }
        return adminAccessToken;
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private record CreatedTournament(long id, String slug) {
    }
}
