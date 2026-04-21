package com.multideporte.backend.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.support.AuthenticatedPostgreSqlIntegrationTestSupport;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BasicStatisticsControllerIntegrationTest extends AuthenticatedPostgreSqlIntegrationTestSupport {

    @Test
    void shouldExposeBasicStatisticsForLeagueScenario() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long tournamentId = createTournament("Statistics Basic " + suffix, "2026-" + suffix);
        transitionTournament(tournamentId, TournamentStatus.OPEN);

        long teamA = createTeam("SB Team A " + suffix, "SBA" + suffix);
        long teamB = createTeam("SB Team B " + suffix, "SBB" + suffix);
        long tournamentTeamA = createTournamentTeam(tournamentId, teamA, 1, 1);
        long tournamentTeamB = createTournamentTeam(tournamentId, teamB, 2, 2);

        long playerA = createPlayer("StatA", suffix, "DOCA" + suffix);
        long playerB = createPlayer("StatB", suffix, "DOCB" + suffix);
        createRoster(tournamentTeamA, playerA, 11);
        createRoster(tournamentTeamB, playerB, 12);

        long stageId = createStage(tournamentId, "Liga " + suffix, "LEAGUE", 1, true);
        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS);

        createMatch(tournamentId, stageId, null, 1, 1, tournamentTeamA, tournamentTeamB, MatchGameStatus.PLAYED.name(), 3, 1);
        createMatch(tournamentId, stageId, null, 1, 2, tournamentTeamB, tournamentTeamA, MatchGameStatus.SCHEDULED.name(), null, null);
        recalculateStanding(tournamentId, stageId, null);

        mockMvc.perform(get("/tournaments/{id}/statistics/basic", tournamentId)
                        .param("stageId", String.valueOf(stageId))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BASIC_STATISTICS_FOUND"))
                .andExpect(jsonPath("$.data.summary.totalMatches").value(2))
                .andExpect(jsonPath("$.data.summary.playedMatches").value(1))
                .andExpect(jsonPath("$.data.summary.scheduledMatches").value(1))
                .andExpect(jsonPath("$.data.summary.scoredPointsFor").value(4))
                .andExpect(jsonPath("$.data.leaders.pointsLeader.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.leaders.pointsLeader.team.tournamentTeamId").value(tournamentTeamA))
                .andExpect(jsonPath("$.data.traceability.classificationSource").value("STANDINGS"));
    }

    @Test
    void shouldReturnPendingLeadersWhenStandingsAreMissing() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        long tournamentId = createTournament("Statistics Pending " + suffix, "2026-" + suffix);
        transitionTournament(tournamentId, TournamentStatus.OPEN);

        long teamA = createTeam("SP Team A " + suffix, "SPA" + suffix);
        long teamB = createTeam("SP Team B " + suffix, "SPB" + suffix);
        long tournamentTeamA = createTournamentTeam(tournamentId, teamA, 1, 1);
        long tournamentTeamB = createTournamentTeam(tournamentId, teamB, 2, 2);

        long playerA = createPlayer("PendA", suffix, "PENA" + suffix);
        long playerB = createPlayer("PendB", suffix, "PENB" + suffix);
        createRoster(tournamentTeamA, playerA, 15);
        createRoster(tournamentTeamB, playerB, 16);

        long stageId = createStage(tournamentId, "Grupos " + suffix, "GROUP_STAGE", 1, true);
        long groupId = createGroup(stageId, "A" + suffix, "Grupo A " + suffix, 1);
        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS);

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamA, tournamentTeamB, MatchGameStatus.PLAYED.name(), 1, 0);

        mockMvc.perform(get("/tournaments/{id}/statistics/basic", tournamentId)
                        .param("stageId", String.valueOf(stageId))
                        .param("groupId", String.valueOf(groupId))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leaders.pointsLeader.status").value("PENDING_RECALCULATION"))
                .andExpect(jsonPath("$.data.traceability.classificationSource").value("STANDINGS_PENDING"));
    }

    private long createTournament(String name, String seasonName) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
                null,
                "Contrato statistics basic",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                8,
                3,
                1,
                0
        );

        return extractId(mockMvc.perform(post("/tournaments")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn());
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

    private long createGroup(long stageId, String code, String name, int sequenceOrder) throws Exception {
        return extractId(mockMvc.perform(post("/stage-groups")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
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

        return extractId(mockMvc.perform(post("/matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void recalculateStanding(long tournamentId, Long stageId, Long groupId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tournamentId", tournamentId);
        payload.put("stageId", stageId);
        payload.put("groupId", groupId);

        mockMvc.perform(post("/standings/recalculate")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}


