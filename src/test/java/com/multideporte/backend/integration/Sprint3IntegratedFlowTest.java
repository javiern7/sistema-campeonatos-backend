package com.multideporte.backend.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.support.PostgreSqlContainerConfig;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class Sprint3IntegratedFlowTest extends PostgreSqlContainerConfig {

    private static final String USERNAME = "devadmin";
    private static final String PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        mockMvc.perform(post("/api/standings/recalculate")
                        .with(httpBasic(USERNAME, PASSWORD))
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

        mockMvc.perform(get("/api/standings")
                        .with(httpBasic(USERNAME, PASSWORD))
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

        mockMvc.perform(post("/api/matches")
                        .with(httpBasic(USERNAME, PASSWORD))
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

        mockMvc.perform(post("/api/standings/recalculate")
                        .with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", stageId,
                                "groupId", groupId
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/teams/{id}", teamId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar el equipo porque ya esta asociado a un torneo"));

        mockMvc.perform(delete("/api/tournament-stages/{id}", stageId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la etapa porque ya tiene grupos asociados"));

        mockMvc.perform(delete("/api/stage-groups/{id}", groupId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar el grupo porque ya tiene partidos o standings asociados"));

        mockMvc.perform(delete("/api/tournament-teams/{id}", tournamentTeamId).with(httpBasic(USERNAME, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede eliminar la inscripcion porque ya tiene partidos o standings asociados"));
    }

    private long createTournament(String name, String seasonName) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
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

        return extractId(mockMvc.perform(post("/api/tournaments")
                        .with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long createTeam(String name, String code, String primaryColor) throws Exception {
        return extractId(mockMvc.perform(post("/api/teams")
                        .with(httpBasic(USERNAME, PASSWORD))
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
        return extractId(mockMvc.perform(post("/api/tournament-teams")
                        .with(httpBasic(USERNAME, PASSWORD))
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
    }

    private long createStage(long tournamentId, String name) throws Exception {
        return extractId(mockMvc.perform(post("/api/tournament-stages")
                        .with(httpBasic(USERNAME, PASSWORD))
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
        return extractId(mockMvc.perform(post("/api/stage-groups")
                        .with(httpBasic(USERNAME, PASSWORD))
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
        return extractId(mockMvc.perform(post("/api/matches")
                        .with(httpBasic(USERNAME, PASSWORD))
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
        mockMvc.perform(post("/api/tournaments/{id}/status-transition", tournamentId)
                        .with(httpBasic(USERNAME, PASSWORD))
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
