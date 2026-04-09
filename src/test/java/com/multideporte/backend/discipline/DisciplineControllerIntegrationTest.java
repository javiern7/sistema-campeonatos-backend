package com.multideporte.backend.discipline;

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
class DisciplineControllerIntegrationTest extends PostgreSqlContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAndReadMatchDisciplineAndTournamentSanctions() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long tournamentId = createTournament("Discipline " + suffix, "2026-" + suffix);
        transitionTournament(tournamentId, TournamentStatus.OPEN);

        long teamA = createTeam("DI Team A " + suffix, "DIA" + suffix);
        long teamB = createTeam("DI Team B " + suffix, "DIB" + suffix);
        long tournamentTeamA = createTournamentTeam(tournamentId, teamA, 1, 1);
        long tournamentTeamB = createTournamentTeam(tournamentId, teamB, 2, 2);

        long playerA = createPlayer("DiscA", suffix, "DIA" + suffix);
        long playerB = createPlayer("DiscB", suffix, "DIB" + suffix);
        createRoster(tournamentTeamA, playerA, 8);
        createRoster(tournamentTeamB, playerB, 9);

        long stageId = createStage(tournamentId, "Liga " + suffix, "LEAGUE", 1, true);
        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS);
        long matchId = createMatch(tournamentId, stageId, null, 1, 1, tournamentTeamA, tournamentTeamB, MatchGameStatus.PLAYED.name(), 2, 1);

        long incidentId = extractId(mockMvc.perform(post("/api/matches/{matchId}/discipline/incidents", matchId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentTeamId", tournamentTeamA,
                                "playerId", playerA,
                                "incidentType", "EXPULSION",
                                "incidentMinute", 78,
                                "notes", "Entrada temeraria"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DISCIPLINARY_INCIDENT_CREATED"))
                .andExpect(jsonPath("$.data.player.playerId").value(playerA))
                .andReturn());

        mockMvc.perform(post("/api/matches/{matchId}/discipline/incidents/{incidentId}/sanctions", matchId, incidentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sanctionType", "SUSPENSION_PROXIMO_PARTIDO",
                                "matchesToServe", 1,
                                "notes", "Suspension simple trazable"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DISCIPLINARY_SANCTION_CREATED"))
                .andExpect(jsonPath("$.data.remainingMatches").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/matches/{matchId}/discipline", matchId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DISCIPLINE_MATCH_FOUND"))
                .andExpect(jsonPath("$.data.incidents[0].incidentType").value("EXPULSION"))
                .andExpect(jsonPath("$.data.sanctions[0].sanctionType").value("SUSPENSION_PROXIMO_PARTIDO"))
                .andExpect(jsonPath("$.data.traceability.matchDerivedFrom").value("MATCH_GAME"));

        mockMvc.perform(get("/api/tournaments/{tournamentId}/discipline/sanctions", tournamentId)
                        .param("activeOnly", "true")
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DISCIPLINARY_SANCTIONS_FOUND"))
                .andExpect(jsonPath("$.data.totalSanctions").value(1))
                .andExpect(jsonPath("$.data.sanctions[0].player.playerId").value(playerA))
                .andExpect(jsonPath("$.data.sanctions[0].team.tournamentTeamId").value(tournamentTeamA));
    }

    @Test
    void shouldRejectIncidentForPlayerOutsideRoster() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long tournamentId = createTournament("Discipline Invalid " + suffix, "2026-" + suffix);
        transitionTournament(tournamentId, TournamentStatus.OPEN);

        long teamA = createTeam("DV Team A " + suffix, "DVA" + suffix);
        long teamB = createTeam("DV Team B " + suffix, "DVB" + suffix);
        long tournamentTeamA = createTournamentTeam(tournamentId, teamA, 1, 1);
        long tournamentTeamB = createTournamentTeam(tournamentId, teamB, 2, 2);

        long playerA = createPlayer("Rostered", suffix, "R" + suffix);
        long playerOutside = createPlayer("Outside", suffix, "O" + suffix);
        createRoster(tournamentTeamA, playerA, 10);

        long stageId = createStage(tournamentId, "Liga " + suffix, "LEAGUE", 1, true);
        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS);
        long matchId = createMatch(tournamentId, stageId, null, 1, 1, tournamentTeamA, tournamentTeamB, MatchGameStatus.PLAYED.name(), 1, 0);

        mockMvc.perform(post("/api/matches/{matchId}/discipline/incidents", matchId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentTeamId", tournamentTeamA,
                                "playerId", playerOutside,
                                "incidentType", "AMONESTACION",
                                "incidentMinute", 20
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    private long createTournament(String name, String seasonName) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
                null,
                "Contrato discipline",
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
                                "primaryColor", "#112233",
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

    private long createMatch(
            long tournamentId,
            long stageId,
            Long groupId,
            int roundNumber,
            int matchdayNumber,
            long homeTournamentTeamId,
            long awayTournamentTeamId,
            String status,
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
        payload.put("status", status);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        payload.put("winnerTournamentTeamId", homeScore != null && awayScore != null && homeScore > awayScore ? homeTournamentTeamId : null);

        return extractId(mockMvc.perform(post("/api/matches")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}
