package com.multideporte.backend.competition;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CompetitionAdvancedControllerIntegrationTest extends PostgreSqlContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeBracketCalendarAndResultsForGroupsThenKnockoutScenario() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long tournamentId = createTournament("Competition Advanced " + suffix, "2026-" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN);

        long[] tournamentTeams = new long[8];
        for (int index = 0; index < 8; index++) {
            char label = (char) ('A' + index);
            long teamId = createTeam("CA Team " + label + " " + suffix, "CA" + label + suffix);
            tournamentTeams[index] = createTournamentTeam(tournamentId, teamId, index + 1, index % 2 == 0 ? 1 : 2);
            long playerId = createPlayer("Player" + label, suffix, "DOC" + suffix + label);
            createRoster(tournamentTeams[index], playerId, 10 + index);
        }

        long groupStageId = createStage(tournamentId, "Grupos " + suffix, "GROUP_STAGE", 1, true);
        long knockoutStageId = createStage(tournamentId, "Knockout " + suffix, "KNOCKOUT", 2, false);
        long[] groups = new long[] {
                createGroup(groupStageId, "A" + suffix, "Grupo A " + suffix, 1),
                createGroup(groupStageId, "B" + suffix, "Grupo B " + suffix, 2),
                createGroup(groupStageId, "C" + suffix, "Grupo C " + suffix, 3),
                createGroup(groupStageId, "D" + suffix, "Grupo D " + suffix, 4)
        };

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS);

        for (int groupIndex = 0; groupIndex < 4; groupIndex++) {
            int teamIndex = groupIndex * 2;
            createMatch(
                    tournamentId,
                    groupStageId,
                    groups[groupIndex],
                    1,
                    groupIndex + 1,
                    tournamentTeams[teamIndex],
                    tournamentTeams[teamIndex + 1],
                    MatchGameStatus.PLAYED.name(),
                    1,
                    0
            );
            recalculateStanding(tournamentId, groupStageId, groups[groupIndex]);
        }

        mockMvc.perform(post("/api/tournaments/{id}/progress-to-knockout", tournamentId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetStageId").value(knockoutStageId))
                .andExpect(jsonPath("$.data.qualifiedTeamsCount").value(4));

        mockMvc.perform(post("/api/tournaments/{id}/generate-knockout-bracket", tournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedMatchesCount").value(2));

        mockMvc.perform(get("/api/tournaments/{id}/competition-advanced/bracket", tournamentId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMPETITION_ADVANCED_BRACKET_FOUND"))
                .andExpect(jsonPath("$.data.stageId").value(knockoutStageId))
                .andExpect(jsonPath("$.data.totalMatches").value(2))
                .andExpect(jsonPath("$.data.rounds[0].matchesCount").value(2));

        mockMvc.perform(get("/api/tournaments/{id}/competition-advanced/calendar", tournamentId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMPETITION_ADVANCED_CALENDAR_FOUND"))
                .andExpect(jsonPath("$.data.totalMatches").value(6))
                .andExpect(jsonPath("$.data.closedMatches").value(4));

        mockMvc.perform(get("/api/tournaments/{id}/competition-advanced/results", tournamentId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMPETITION_ADVANCED_RESULTS_FOUND"))
                .andExpect(jsonPath("$.data.totalClosedMatches").value(4))
                .andExpect(jsonPath("$.data.results[0].affectsStandings").value(true))
                .andExpect(jsonPath("$.data.results[0].standingScope").value("GROUP"));
    }

    private long createTournament(String name, String seasonName) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.GROUPS_THEN_KNOCKOUT,
                TournamentStatus.DRAFT,
                null,
                "Contrato competition advanced",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                8,
                3,
                1,
                0
        );

        return extractId(mockMvc.perform(post("/api/tournaments")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void transitionTournament(long tournamentId, TournamentStatus status) throws Exception {
        mockMvc.perform(post("/api/tournaments/{id}/status-transition", tournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", status.name()))))
                .andExpect(status().isOk());
    }

    private long createTeam(String name, String code) throws Exception {
        return extractId(mockMvc.perform(post("/api/teams")
                        .with(httpBasic("devadmin", "admin123"))
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
        return extractId(mockMvc.perform(post("/api/tournament-teams")
                        .with(httpBasic("devadmin", "admin123"))
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
        return extractId(mockMvc.perform(post("/api/players")
                        .with(httpBasic("devadmin", "admin123"))
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
        return extractId(mockMvc.perform(post("/api/rosters")
                        .with(httpBasic("devadmin", "admin123"))
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
        return extractId(mockMvc.perform(post("/api/tournament-stages")
                        .with(httpBasic("devadmin", "admin123"))
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

    private long createGroup(long stageId, String code, String name, int sequenceOrder) throws Exception {
        return extractId(mockMvc.perform(post("/api/stage-groups")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "stageId", stageId,
                                "code", code,
                                "name", name,
                                "sequenceOrder", sequenceOrder
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createMatch(
            long tournamentId,
            long stageId,
            long groupId,
            int roundNumber,
            int matchdayNumber,
            long homeTournamentTeamId,
            long awayTournamentTeamId,
            String status,
            int homeScore,
            int awayScore
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tournamentId", tournamentId);
        payload.put("stageId", stageId);
        payload.put("groupId", groupId);
        payload.put("roundNumber", roundNumber);
        payload.put("matchdayNumber", matchdayNumber);
        payload.put("homeTournamentTeamId", homeTournamentTeamId);
        payload.put("awayTournamentTeamId", awayTournamentTeamId);
        payload.put("status", status);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        payload.put("winnerTournamentTeamId", homeTournamentTeamId);

        return extractId(mockMvc.perform(post("/api/matches")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void recalculateStanding(long tournamentId, long stageId, long groupId) throws Exception {
        mockMvc.perform(post("/api/standings/recalculate")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId
                        ))))
                .andExpect(status().isOk());
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}
