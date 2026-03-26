package com.multideporte.backend.tournament;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.support.PostgreSqlContainerConfig;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.entity.TournamentFormat;
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
class TournamentControllerIntegrationTest extends PostgreSqlContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateTournamentSuccessfully() throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                "Liga Test Integracion",
                "2026",
                TournamentFormat.LEAGUE,
                TournamentStatus.DRAFT,
                "Torneo para validar flujo completo",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                10,
                3,
                1,
                0
        );

        mockMvc.perform(post("/api/tournaments")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.slug").value("liga-test-integracion-2026"))
                .andExpect(jsonPath("$.data.createdByUserId").isNumber());
    }

    @Test
    void shouldListSeededSports() throws Exception {
        mockMvc.perform(get("/api/sports")
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").exists());
    }

    @Test
    void shouldTransitionTournamentThroughOperationalLifecycle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Lifecycle " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);
        long teamAId = createTeam("Lifecycle Team A " + suffix, "LTA" + suffix);
        long teamBId = createTeam("Lifecycle Team B " + suffix, "LTB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.OPEN.name()));

        createTournamentTeam(tournamentId, teamAId, 1, 1);
        createTournamentTeam(tournamentId, teamBId, 2, 2);
        createStage(tournamentId, "Liga " + suffix, "GROUP_STAGE");

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.IN_PROGRESS.name()));
    }

    @Test
    void shouldRejectDirectStatusMutationThroughUpdateEndpoint() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Update Guard " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);

        mockMvc.perform(put("/api/tournaments/{id}", tournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("sportId", 1),
                                Map.entry("name", "Update Guard " + suffix),
                                Map.entry("seasonName", "2026-" + suffix),
                                Map.entry("format", TournamentFormat.LEAGUE.name()),
                                Map.entry("status", TournamentStatus.OPEN.name()),
                                Map.entry("description", "Cambio invalido de estado"),
                                Map.entry("startDate", "2026-04-01"),
                                Map.entry("endDate", "2026-06-30"),
                                Map.entry("registrationOpenAt", "2026-03-20T10:00:00Z"),
                                Map.entry("registrationCloseAt", "2026-03-31T23:59:59Z"),
                                Map.entry("maxTeams", 10),
                                Map.entry("pointsWin", 3),
                                Map.entry("pointsDraw", 1),
                                Map.entry("pointsLoss", 0)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El status del torneo debe cambiarse usando el endpoint de transicion"));
    }

    @Test
    void shouldRejectStartingGroupsThenKnockoutTournamentWithoutKnockoutStage() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Format Guard " + suffix, "2026-" + suffix, TournamentFormat.GROUPS_THEN_KNOCKOUT);
        long teamAId = createTeam("Format Team A " + suffix, "FTA" + suffix);
        long teamBId = createTeam("Format Team B " + suffix, "FTB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        createTournamentTeam(tournamentId, teamAId, 1, 1);
        createTournamentTeam(tournamentId, teamBId, 2, 2);
        createStage(tournamentId, "Solo grupos " + suffix, "GROUP_STAGE");

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Un torneo GROUPS_THEN_KNOCKOUT requiere fases GROUP_STAGE y KNOCKOUT para iniciar"));
    }

    @Test
    void shouldFreezeStructureAfterTournamentStartsAndAllowFinishingWhenOperationallyConsistent() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Finish Flow " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);
        long teamAId = createTeam("Finish Team A " + suffix, "FNA" + suffix);
        long teamBId = createTeam("Finish Team B " + suffix, "FNB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long stageId = createStage(tournamentId, "Liga principal " + suffix, "GROUP_STAGE");
        long groupId = createGroup(stageId, "A" + suffix, "Grupo A " + suffix);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tournament-stages")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "name", "Etapa tardia " + suffix,
                                "stageType", "GROUP_STAGE",
                                "sequenceOrder", 2,
                                "legs", 1,
                                "roundTrip", false,
                                "active", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite modificar la estructura de un torneo en progreso, finalizado o cancelado"));

        mockMvc.perform(post("/api/tournament-teams")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "teamId", createTeam("Extra Team " + suffix, "EXT" + suffix),
                                "registrationStatus", "APPROVED",
                                "seedNumber", 3,
                                "groupDrawPosition", 3
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite modificar inscripciones en un torneo en progreso, cancelado o finalizado"));

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.PLAYED.name(), 2, 1);

        transitionTournament(tournamentId, TournamentStatus.FINISHED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.FINISHED.name()));
    }

    @Test
    void shouldRejectFinishingTournamentWithScheduledMatchesPending() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Scheduled Guard " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);
        long teamAId = createTeam("Scheduled Team A " + suffix, "STA" + suffix);
        long teamBId = createTeam("Scheduled Team B " + suffix, "STB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long stageId = createStage(tournamentId, "Liga schedule " + suffix, "GROUP_STAGE");
        long groupId = createGroup(stageId, "B" + suffix, "Grupo B " + suffix);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.SCHEDULED.name(), null, null);

        transitionTournament(tournamentId, TournamentStatus.FINISHED)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se puede finalizar un torneo sin partidos cerrados"));
    }

    @Test
    void shouldAllowCancellingTournamentBeforeItStartsAndRejectFurtherChanges() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Cancel Guard " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.OPEN.name()));

        transitionTournament(tournamentId, TournamentStatus.CANCELLED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.CANCELLED.name()));

        mockMvc.perform(post("/api/tournament-stages")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "name", "Etapa cancelada " + suffix,
                                "stageType", "GROUP_STAGE",
                                "sequenceOrder", 1,
                                "legs", 1,
                                "roundTrip", false,
                                "active", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite modificar la estructura de un torneo en progreso, finalizado o cancelado"));

        mockMvc.perform(put("/api/tournaments/{id}", tournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("sportId", 1),
                                Map.entry("name", "Cancel Guard " + suffix),
                                Map.entry("seasonName", "2026-" + suffix),
                                Map.entry("format", TournamentFormat.LEAGUE.name()),
                                Map.entry("status", TournamentStatus.CANCELLED.name()),
                                Map.entry("description", "Intento de editar cancelado"),
                                Map.entry("startDate", "2026-04-01"),
                                Map.entry("endDate", "2026-06-30"),
                                Map.entry("registrationOpenAt", "2026-03-20T10:00:00Z"),
                                Map.entry("registrationCloseAt", "2026-03-31T23:59:59Z"),
                                Map.entry("maxTeams", 10),
                                Map.entry("pointsWin", 3),
                                Map.entry("pointsDraw", 1),
                                Map.entry("pointsLoss", 0)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite actualizar un torneo finalizado o cancelado"));
    }

    @Test
    void shouldRejectCancellingTournamentAlreadyInProgress() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Cancel Late " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);
        long teamAId = createTeam("Cancel Late A " + suffix, "CLA" + suffix);
        long teamBId = createTeam("Cancel Late B " + suffix, "CLB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        createTournamentTeam(tournamentId, teamAId, 1, 1);
        createTournamentTeam(tournamentId, teamBId, 2, 2);
        createStage(tournamentId, "Liga cancel late " + suffix, "GROUP_STAGE");

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        transitionTournament(tournamentId, TournamentStatus.CANCELLED)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Solo un torneo DRAFT u OPEN puede pasar a CANCELLED"));
    }

    private long createTournament(String name, String seasonName, TournamentFormat format) throws Exception {
        TournamentCreateRequest request = new TournamentCreateRequest(
                1L,
                name,
                seasonName,
                format,
                TournamentStatus.DRAFT,
                "Torneo para validar lifecycle",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30),
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                OffsetDateTime.parse("2026-03-31T23:59:59Z"),
                10,
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

    private long createStage(long tournamentId, String name, String stageType) throws Exception {
        return extractId(mockMvc.perform(post("/api/tournament-stages")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "name", name,
                                "stageType", stageType,
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
                        .with(httpBasic("devadmin", "admin123"))
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
            String matchStatus,
            Integer homeScore,
            Integer awayScore
    ) throws Exception {
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("tournamentId", tournamentId);
        payload.put("stageId", stageId);
        payload.put("groupId", groupId);
        payload.put("roundNumber", roundNumber);
        payload.put("matchdayNumber", matchdayNumber);
        payload.put("homeTournamentTeamId", homeTournamentTeamId);
        payload.put("awayTournamentTeamId", awayTournamentTeamId);
        payload.put("status", matchStatus);
        payload.put("homeScore", homeScore);
        payload.put("awayScore", awayScore);
        if (homeScore != null && awayScore != null && !homeScore.equals(awayScore)) {
            payload.put("winnerTournamentTeamId", homeScore > awayScore ? homeTournamentTeamId : awayTournamentTeamId);
        }

        return extractId(mockMvc.perform(post("/api/matches")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private org.springframework.test.web.servlet.ResultActions transitionTournament(long tournamentId, TournamentStatus status) throws Exception {
        return mockMvc.perform(post("/api/tournaments/{id}/status-transition", tournamentId)
                .with(httpBasic("devadmin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("targetStatus", status.name()))));
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }
}
