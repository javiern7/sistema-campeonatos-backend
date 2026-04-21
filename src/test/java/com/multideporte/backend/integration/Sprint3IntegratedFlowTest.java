package com.multideporte.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.support.AuthenticatedPostgreSqlIntegrationTestSupport;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class Sprint3IntegratedFlowTest extends AuthenticatedPostgreSqlIntegrationTestSupport {

    @Test
    void shouldExecuteIntegratedFlowAndGenerateStableStandings() throws Exception {
        String suffix = uniqueSuffix();

        long tournamentId = createTournament("Sprint 3 League " + suffix, "2026-" + suffix);
        long teamAId = createTeam("Team Alpha " + suffix, "ALP" + suffix, "#AA1100");
        long teamBId = createTeam("Team Beta " + suffix, "BET" + suffix, "#00AA11");
        long teamCId = createTeam("Team Gamma " + suffix, "GAM" + suffix, "#0011AA");

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long tournamentTeamCId = createTournamentTeam(tournamentId, teamCId, 3, 3);

        long stageId = createStage(tournamentId, "Fase de Grupos " + suffix);
        long groupId = createGroup(stageId, "a" + suffix, "Grupo " + suffix);

        transitionTournament(tournamentId, "OPEN");
        transitionTournament(tournamentId, "IN_PROGRESS");

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamAId, tournamentTeamBId, 3, 0);
        createMatch(tournamentId, stageId, groupId, 1, 2, tournamentTeamCId, tournamentTeamAId, 1, 0);
        createMatch(tournamentId, stageId, groupId, 1, 3, tournamentTeamBId, tournamentTeamCId, 2, 0);

        mockMvc.perform(post("/standings/recalculate")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matchesProcessed").value(3))
                .andExpect(jsonPath("$.data.standingsGenerated").value(3));

        mockMvc.perform(get("/standings")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .param("tournamentId", String.valueOf(tournamentId))
                        .param("stageId", String.valueOf(stageId))
                        .param("groupId", String.valueOf(groupId))
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].tournamentTeamId").value(tournamentTeamAId))
                .andExpect(jsonPath("$.data.content[0].rankPosition").value(1))
                .andExpect(jsonPath("$.data.content[0].points").value(3))
                .andExpect(jsonPath("$.data.content[0].scoreDiff").value(2))
                .andExpect(jsonPath("$.data.content[1].tournamentTeamId").value(tournamentTeamBId))
                .andExpect(jsonPath("$.data.content[1].rankPosition").value(2))
                .andExpect(jsonPath("$.data.content[1].points").value(3))
                .andExpect(jsonPath("$.data.content[1].scoreDiff").value(-1))
                .andExpect(jsonPath("$.data.content[1].pointsFor").value(2))
                .andExpect(jsonPath("$.data.content[2].tournamentTeamId").value(tournamentTeamCId))
                .andExpect(jsonPath("$.data.content[2].rankPosition").value(3))
                .andExpect(jsonPath("$.data.content[2].points").value(3))
                .andExpect(jsonPath("$.data.content[2].scoreDiff").value(-1))
                .andExpect(jsonPath("$.data.content[2].pointsFor").value(1));
    }

    @Test
    void shouldRejectDuplicateMatchWithinSameScope() throws Exception {
        String suffix = uniqueSuffix();

        long tournamentId = createTournament("Sprint 3 Duplicate " + suffix, "2026-DUP-" + suffix);
        long teamAId = createTeam("Dup Team A " + suffix, "DPA" + suffix, "#663399");
        long teamBId = createTeam("Dup Team B " + suffix, "DPB" + suffix, "#339966");
        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long stageId = createStage(tournamentId, "Dup Stage " + suffix);
        long groupId = createGroup(stageId, "b" + suffix, "Grupo B " + suffix);

        transitionTournament(tournamentId, "OPEN");
        transitionTournament(tournamentId, "IN_PROGRESS");

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamAId, tournamentTeamBId, 2, 1);

        mockMvc.perform(post("/matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId,
                                "roundNumber", 1,
                                "matchdayNumber", 1,
                                "homeTournamentTeamId", tournamentTeamAId,
                                "awayTournamentTeamId", tournamentTeamBId,
                                "status", MatchGameStatus.PLAYED.name(),
                                "homeScore", 2,
                                "awayScore", 1
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("Ya existe un partido con el mismo cruce, roundNumber y matchdayNumber en ese alcance"));
    }

    @Test
    void shouldPreventDeletingEntitiesWithActiveDependencies() throws Exception {
        String suffix = uniqueSuffix();

        long tournamentId = createTournament("Sprint 3 Guardrails " + suffix, "2026-GRD-" + suffix);
        long teamId = createTeam("Guard Team " + suffix, "GRD" + suffix, "#AA55CC");
        long otherTeamId = createTeam("Guard Team 2 " + suffix, "GR2" + suffix, "#55CCAA");
        long tournamentTeamId = createTournamentTeam(tournamentId, teamId, 1, 1);
        long otherTournamentTeamId = createTournamentTeam(tournamentId, otherTeamId, 2, 2);
        long stageId = createStage(tournamentId, "Guard Stage " + suffix);
        long groupId = createGroup(stageId, "c" + suffix, "Grupo C " + suffix);

        transitionTournament(tournamentId, "OPEN");
        transitionTournament(tournamentId, "IN_PROGRESS");

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamId, otherTournamentTeamId, 1, 0);

        mockMvc.perform(post("/standings/recalculate")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/teams/{id}", teamId).header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar el equipo porque ya esta asociado a un torneo"));

        mockMvc.perform(delete("/tournament-stages/{id}", stageId).header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite modificar la estructura de un torneo en progreso, finalizado o cancelado"));

        mockMvc.perform(delete("/stage-groups/{id}", groupId).header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite modificar la estructura de un torneo en progreso, finalizado o cancelado"));

        mockMvc.perform(delete("/tournament-teams/{id}", tournamentTeamId).header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la inscripcion porque ya tiene jugadores en roster"));
    }

    private long createTournament(String name, String seasonName) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
                null,
                "Flujo integrado Sprint 3",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                12,
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

    private long createTeam(String name, String code, String primaryColor) throws Exception {
        return extractId(mockMvc.perform(post("/teams")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "shortName", name.substring(0, Math.min(12, name.length())),
                                "code", code,
                                "primaryColor", primaryColor,
                                "secondaryColor", "#FFFFFF",
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createTournamentTeam(long tournamentId, long teamId, int seedNumber, int groupDrawPosition) throws Exception {
        long tournamentTeamId = extractId(mockMvc.perform(post("/tournament-teams")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "teamId", teamId,
                                "registrationStatus", TournamentTeamRegistrationStatus.APPROVED.name(),
                                "seedNumber", seedNumber,
                                "groupDrawPosition", groupDrawPosition
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
        long playerId = createPlayer("Auto", "Sprint" + tournamentTeamId, "SP" + tournamentTeamId + uniqueSuffix());
        createRoster(tournamentTeamId, playerId, 60 + (int) (tournamentTeamId % 30));
        return tournamentTeamId;
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

    private long createStage(long tournamentId, String name) throws Exception {
        return extractId(mockMvc.perform(post("/tournament-stages")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "name", name,
                                "stageType", TournamentStageType.GROUP_STAGE.name(),
                                "sequenceOrder", 1,
                                "legs", 1,
                                "roundTrip", false,
                                "active", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createGroup(long stageId, String code, String name) throws Exception {
        return extractId(mockMvc.perform(post("/stage-groups")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "stageId", stageId,
                                "code", code,
                                "name", name,
                                "sequenceOrder", 1
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
            int homeScore,
            int awayScore
    ) throws Exception {
        return extractId(mockMvc.perform(post("/matches")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId,
                                "roundNumber", roundNumber,
                                "matchdayNumber", matchdayNumber,
                                "homeTournamentTeamId", homeTournamentTeamId,
                                "awayTournamentTeamId", awayTournamentTeamId,
                                "status", MatchGameStatus.PLAYED.name(),
                                "homeScore", homeScore,
                                "awayScore", awayScore
                        ))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void transitionTournament(long tournamentId, String targetStatus) throws Exception {
        mockMvc.perform(post("/tournaments/{id}/status-transition", tournamentId)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(adminAccessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetStatus", targetStatus))))
                .andExpect(status().isOk());
    }

    private long extractId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}


