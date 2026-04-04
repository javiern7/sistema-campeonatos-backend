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
import com.multideporte.backend.roster.entity.RosterStatus;
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
                null,
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
                .andExpect(jsonPath("$.data.createdByUserId").isNumber())
                .andExpect(jsonPath("$.data.operationalCategory").value(TournamentOperationalCategory.PRODUCTION.name()))
                .andExpect(jsonPath("$.data.executiveReportingEligible").value(true));
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
    void shouldProgressGroupsThenKnockoutTournamentAndOnlyAllowQualifiedTeamsInKnockout() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Progression " + suffix, "2026-" + suffix, TournamentFormat.GROUPS_THEN_KNOCKOUT);
        long teamAId = createTeam("Progress Team A " + suffix, "PTA" + suffix);
        long teamBId = createTeam("Progress Team B " + suffix, "PTB" + suffix);
        long teamCId = createTeam("Progress Team C " + suffix, "PTC" + suffix);
        long teamDId = createTeam("Progress Team D " + suffix, "PTD" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long tournamentTeamCId = createTournamentTeam(tournamentId, teamCId, 3, 1);
        long tournamentTeamDId = createTournamentTeam(tournamentId, teamDId, 4, 2);

        long groupStageId = createStage(tournamentId, "Fase grupos " + suffix, "GROUP_STAGE", 1, true);
        long knockoutStageId = createStage(tournamentId, "Fase final " + suffix, "KNOCKOUT", 2, false);
        long groupAId = createGroup(groupStageId, "A" + suffix, "Grupo A " + suffix, 1);
        long groupBId = createGroup(groupStageId, "B" + suffix, "Grupo B " + suffix, 2);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        createMatch(tournamentId, groupStageId, groupAId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.PLAYED.name(), 3, 1);
        createMatch(tournamentId, groupStageId, groupBId, 1, 1, tournamentTeamCId, tournamentTeamDId, MatchGameStatus.PLAYED.name(), 2, 0);

        recalculateStanding(tournamentId, groupStageId, groupAId);
        recalculateStanding(tournamentId, groupStageId, groupBId);

        progressToKnockout(tournamentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceStageId").value(groupStageId))
                .andExpect(jsonPath("$.data.targetStageId").value(knockoutStageId))
                .andExpect(jsonPath("$.data.qualifiedTeamsCount").value(2))
                .andExpect(jsonPath("$.data.qualifiedTournamentTeamIds[0]").value(tournamentTeamAId))
                .andExpect(jsonPath("$.data.qualifiedTournamentTeamIds[1]").value(tournamentTeamCId));

        mockMvc.perform(post("/api/matches")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentId", tournamentId,
                                "stageId", knockoutStageId,
                                "roundNumber", 1,
                                "matchdayNumber", 1,
                                "homeTournamentTeamId", tournamentTeamAId,
                                "awayTournamentTeamId", tournamentTeamBId,
                                "status", MatchGameStatus.SCHEDULED.name()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Los partidos KNOCKOUT solo pueden involucrar equipos clasificados desde grupos"));

        createMatch(tournamentId, knockoutStageId, null, 1, 1, tournamentTeamAId, tournamentTeamCId, MatchGameStatus.PLAYED.name(), 2, 1);

        transitionTournament(tournamentId, TournamentStatus.FINISHED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(TournamentStatus.FINISHED.name()));
    }

    @Test
    void shouldRejectProgressingGroupsThenKnockoutWithoutGroupStandings() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Progression Guard " + suffix, "2026-" + suffix, TournamentFormat.GROUPS_THEN_KNOCKOUT);
        long teamAId = createTeam("Guard Team A " + suffix, "GTA" + suffix);
        long teamBId = createTeam("Guard Team B " + suffix, "GTB" + suffix);
        long teamCId = createTeam("Guard Team C " + suffix, "GTC" + suffix);
        long teamDId = createTeam("Guard Team D " + suffix, "GTD" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long tournamentTeamCId = createTournamentTeam(tournamentId, teamCId, 3, 1);
        long tournamentTeamDId = createTournamentTeam(tournamentId, teamDId, 4, 2);

        long groupStageId = createStage(tournamentId, "Grupos guard " + suffix, "GROUP_STAGE", 1, true);
        createStage(tournamentId, "Knockout guard " + suffix, "KNOCKOUT", 2, false);
        long groupAId = createGroup(groupStageId, "A" + suffix, "Grupo A " + suffix, 1);
        long groupBId = createGroup(groupStageId, "B" + suffix, "Grupo B " + suffix, 2);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        createMatch(tournamentId, groupStageId, groupAId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.PLAYED.name(), 1, 0);
        createMatch(tournamentId, groupStageId, groupBId, 1, 1, tournamentTeamCId, tournamentTeamDId, MatchGameStatus.PLAYED.name(), 1, 0);

        progressToKnockout(tournamentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se puede progresar a KNOCKOUT sin standings recalculados por grupo"));
    }

    @Test
    void shouldGenerateInitialKnockoutBracketUsingDefaultGroupRankSeeding() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BracketScenario scenario = createKnockoutBracketScenario(suffix, 4, 1, 3, 2);

        generateKnockoutBracket(scenario.tournamentId(), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageId").value(scenario.knockoutStageId()))
                .andExpect(jsonPath("$.data.seedingStrategy").value("GROUP_RANK"))
                .andExpect(jsonPath("$.data.roundNumber").value(1))
                .andExpect(jsonPath("$.data.matchdayNumber").value(1))
                .andExpect(jsonPath("$.data.generatedMatchesCount").value(2))
                .andExpect(jsonPath("$.data.generatedMatches[0].homeTournamentTeamId").value(scenario.qualifiedTeamAId()))
                .andExpect(jsonPath("$.data.generatedMatches[0].awayTournamentTeamId").value(scenario.qualifiedTeamGId()))
                .andExpect(jsonPath("$.data.generatedMatches[0].homeSeedPosition").value(1))
                .andExpect(jsonPath("$.data.generatedMatches[0].awaySeedPosition").value(4))
                .andExpect(jsonPath("$.data.generatedMatches[1].homeTournamentTeamId").value(scenario.qualifiedTeamCId()))
                .andExpect(jsonPath("$.data.generatedMatches[1].awayTournamentTeamId").value(scenario.qualifiedTeamEId()))
                .andExpect(jsonPath("$.data.generatedMatches[1].homeSeedPosition").value(2))
                .andExpect(jsonPath("$.data.generatedMatches[1].awaySeedPosition").value(3));
    }

    @Test
    void shouldGenerateInitialKnockoutBracketUsingTournamentSeedStrategy() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BracketScenario scenario = createKnockoutBracketScenario(suffix, 4, 1, 3, 2);

        generateKnockoutBracket(scenario.tournamentId(), Map.of(
                "seedingStrategy", "TOURNAMENT_SEED",
                "roundNumber", 1,
                "matchdayNumber", 1
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageId").value(scenario.knockoutStageId()))
                .andExpect(jsonPath("$.data.seedingStrategy").value("TOURNAMENT_SEED"))
                .andExpect(jsonPath("$.data.generatedMatchesCount").value(2))
                .andExpect(jsonPath("$.data.generatedMatches[0].homeTournamentTeamId").value(scenario.qualifiedTeamCId()))
                .andExpect(jsonPath("$.data.generatedMatches[0].awayTournamentTeamId").value(scenario.qualifiedTeamAId()))
                .andExpect(jsonPath("$.data.generatedMatches[0].homeSeedPosition").value(1))
                .andExpect(jsonPath("$.data.generatedMatches[0].awaySeedPosition").value(4))
                .andExpect(jsonPath("$.data.generatedMatches[1].homeTournamentTeamId").value(scenario.qualifiedTeamGId()))
                .andExpect(jsonPath("$.data.generatedMatches[1].awayTournamentTeamId").value(scenario.qualifiedTeamEId()))
                .andExpect(jsonPath("$.data.generatedMatches[1].homeSeedPosition").value(2))
                .andExpect(jsonPath("$.data.generatedMatches[1].awaySeedPosition").value(3));
    }

    @Test
    void shouldRejectRegeneratingKnockoutBracketWhenStageAlreadyHasMatches() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BracketScenario scenario = createKnockoutBracketScenario(suffix, 1, 2, 3, 4);

        generateKnockoutBracket(scenario.tournamentId(), Map.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedMatchesCount").value(2));

        generateKnockoutBracket(scenario.tournamentId(), Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No se permite regenerar el bracket KNOCKOUT cuando la etapa ya tiene partidos cargados"));
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
    void shouldFilterTournamentsByOperationalCategoryAndExecutiveOnly() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        createTournament("Production " + suffix, "2026-P-" + suffix, TournamentFormat.LEAGUE);

        long qaTournamentId = createTournament("QA " + suffix, "2026-QA-" + suffix, TournamentFormat.LEAGUE);
        mockMvc.perform(put("/api/tournaments/{id}", qaTournamentId)
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("sportId", 1),
                                Map.entry("name", "QA " + suffix),
                                Map.entry("seasonName", "2026-QA-" + suffix),
                                Map.entry("format", TournamentFormat.LEAGUE.name()),
                                Map.entry("status", TournamentStatus.DRAFT.name()),
                                Map.entry("operationalCategory", TournamentOperationalCategory.QA.name()),
                                Map.entry("description", "Torneo QA"),
                                Map.entry("startDate", "2026-04-01"),
                                Map.entry("endDate", "2026-06-30"),
                                Map.entry("registrationOpenAt", "2026-03-20T10:00:00Z"),
                                Map.entry("registrationCloseAt", "2026-03-31T23:59:59Z"),
                                Map.entry("maxTeams", 10),
                                Map.entry("pointsWin", 3),
                                Map.entry("pointsDraw", 1),
                                Map.entry("pointsLoss", 0)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operationalCategory").value(TournamentOperationalCategory.QA.name()))
                .andExpect(jsonPath("$.data.executiveReportingEligible").value(false));

        mockMvc.perform(get("/api/tournaments")
                        .with(httpBasic("devadmin", "admin123"))
                        .param("operationalCategory", TournamentOperationalCategory.QA.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(qaTournamentId))
                .andExpect(jsonPath("$.data.content[0].operationalCategory").value(TournamentOperationalCategory.QA.name()))
                .andExpect(jsonPath("$.data.content[0].executiveReportingEligible").value(false));

        mockMvc.perform(get("/api/tournaments")
                        .with(httpBasic("devadmin", "admin123"))
                        .param("executiveOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].operationalCategory").value(org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.is(TournamentOperationalCategory.PRODUCTION.name())
                )));
    }

    @Test
    void shouldExposeOperationalSummaryWithIntegrityAlerts() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long tournamentId = createTournament("Ops Summary " + suffix, "2026-" + suffix, TournamentFormat.LEAGUE);
        long teamAId = createTeam("Ops Team A " + suffix, "OSA" + suffix);
        long teamBId = createTeam("Ops Team B " + suffix, "OSB" + suffix);

        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, 1, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 2, 2);
        long stageId = createStage(tournamentId, "Resumen " + suffix, "GROUP_STAGE");
        long groupId = createGroup(stageId, "OPS" + suffix, "Grupo OPS " + suffix);

        createRosterEntry(tournamentTeamAId, 10);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        createMatch(tournamentId, stageId, groupId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.PLAYED.name(), 2, 1);

        mockMvc.perform(get("/api/tournaments/{id}/operational-summary", tournamentId)
                        .with(httpBasic("devadmin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tournamentId").value(tournamentId))
                .andExpect(jsonPath("$.data.operationalCategory").value(TournamentOperationalCategory.PRODUCTION.name()))
                .andExpect(jsonPath("$.data.executiveReportingEligible").value(true))
                .andExpect(jsonPath("$.data.integrityHealthy").value(false))
                .andExpect(jsonPath("$.data.approvedTeams").value(2))
                .andExpect(jsonPath("$.data.approvedTeamsWithActiveRosterSupport").value(1))
                .andExpect(jsonPath("$.data.approvedTeamsMissingActiveRosterSupport").value(1))
                .andExpect(jsonPath("$.data.closedMatches").value(1))
                .andExpect(jsonPath("$.data.generatedStandings").value(0))
                .andExpect(jsonPath("$.data.integrityAlerts").isArray())
                .andExpect(jsonPath("$.data.integrityAlerts[*]").value(org.hamcrest.Matchers.hasItems(
                        "APPROVED_TEAMS_MISSING_ACTIVE_ROSTER_SUPPORT",
                        "CLOSED_MATCHES_WITHOUT_FULL_ACTIVE_ROSTER_SUPPORT",
                        "CLOSED_MATCHES_WITHOUT_STANDINGS"
                )));

        mockMvc.perform(get("/api/tournaments/operational-summary")
                        .with(httpBasic("devadmin", "admin123"))
                        .param("name", "Ops Summary " + suffix)
                        .param("executiveOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].tournamentId").value(tournamentId))
                .andExpect(jsonPath("$.data.content[0].integrityHealthy").value(false));
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
                null,
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

    private long createRosterEntry(long tournamentTeamId, int jerseyNumber) throws Exception {
        return extractId(mockMvc.perform(post("/api/rosters")
                        .with(httpBasic("devadmin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tournamentTeamId", tournamentTeamId,
                                "playerId", createPlayer("Player " + tournamentTeamId + "-" + jerseyNumber, "Roster", "DOC" + UUID.randomUUID().toString().substring(0, 6)),
                                "jerseyNumber", jerseyNumber,
                                "captain", false,
                                "positionName", "MID",
                                "rosterStatus", RosterStatus.ACTIVE.name(),
                                "startDate", "2026-04-01"
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
        return createStage(tournamentId, name, stageType, 1, true);
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

    private long createGroup(long stageId, String code, String name) throws Exception {
        return createGroup(stageId, code, name, 1);
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

    private long createMatch(
            long tournamentId,
            long stageId,
            Long groupId,
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

    private org.springframework.test.web.servlet.ResultActions progressToKnockout(long tournamentId) throws Exception {
        return mockMvc.perform(post("/api/tournaments/{id}/progress-to-knockout", tournamentId)
                .with(httpBasic("devadmin", "admin123")));
    }

    private org.springframework.test.web.servlet.ResultActions generateKnockoutBracket(
            long tournamentId,
            Map<String, Object> payload
    ) throws Exception {
        return mockMvc.perform(post("/api/tournaments/{id}/generate-knockout-bracket", tournamentId)
                .with(httpBasic("devadmin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private BracketScenario createKnockoutBracketScenario(
            String suffix,
            int seedA,
            int seedC,
            int seedE,
            int seedG
    ) throws Exception {
        long tournamentId = createTournament("Bracket " + suffix, "2026-" + suffix, TournamentFormat.GROUPS_THEN_KNOCKOUT);
        transitionTournament(tournamentId, TournamentStatus.OPEN)
                .andExpect(status().isOk());

        long teamAId = createTeam("Bracket Team A " + suffix, "BTA" + suffix);
        long teamBId = createTeam("Bracket Team B " + suffix, "BTB" + suffix);
        long teamCId = createTeam("Bracket Team C " + suffix, "BTC" + suffix);
        long teamDId = createTeam("Bracket Team D " + suffix, "BTD" + suffix);
        long teamEId = createTeam("Bracket Team E " + suffix, "BTE" + suffix);
        long teamFId = createTeam("Bracket Team F " + suffix, "BTF" + suffix);
        long teamGId = createTeam("Bracket Team G " + suffix, "BTG" + suffix);
        long teamHId = createTeam("Bracket Team H " + suffix, "BTH" + suffix);

        long tournamentTeamAId = createTournamentTeam(tournamentId, teamAId, seedA, 1);
        long tournamentTeamBId = createTournamentTeam(tournamentId, teamBId, 91, 2);
        long tournamentTeamCId = createTournamentTeam(tournamentId, teamCId, seedC, 1);
        long tournamentTeamDId = createTournamentTeam(tournamentId, teamDId, 92, 2);
        long tournamentTeamEId = createTournamentTeam(tournamentId, teamEId, seedE, 1);
        long tournamentTeamFId = createTournamentTeam(tournamentId, teamFId, 93, 2);
        long tournamentTeamGId = createTournamentTeam(tournamentId, teamGId, seedG, 1);
        long tournamentTeamHId = createTournamentTeam(tournamentId, teamHId, 94, 2);

        long groupStageId = createStage(tournamentId, "Grupos bracket " + suffix, "GROUP_STAGE", 1, true);
        long knockoutStageId = createStage(tournamentId, "Knockout bracket " + suffix, "KNOCKOUT", 2, false);

        long groupAId = createGroup(groupStageId, "A" + suffix, "Grupo A " + suffix, 1);
        long groupBId = createGroup(groupStageId, "B" + suffix, "Grupo B " + suffix, 2);
        long groupCId = createGroup(groupStageId, "C" + suffix, "Grupo C " + suffix, 3);
        long groupDId = createGroup(groupStageId, "D" + suffix, "Grupo D " + suffix, 4);

        transitionTournament(tournamentId, TournamentStatus.IN_PROGRESS)
                .andExpect(status().isOk());

        createMatch(tournamentId, groupStageId, groupAId, 1, 1, tournamentTeamAId, tournamentTeamBId, MatchGameStatus.PLAYED.name(), 1, 0);
        createMatch(tournamentId, groupStageId, groupBId, 1, 1, tournamentTeamCId, tournamentTeamDId, MatchGameStatus.PLAYED.name(), 1, 0);
        createMatch(tournamentId, groupStageId, groupCId, 1, 1, tournamentTeamEId, tournamentTeamFId, MatchGameStatus.PLAYED.name(), 1, 0);
        createMatch(tournamentId, groupStageId, groupDId, 1, 1, tournamentTeamGId, tournamentTeamHId, MatchGameStatus.PLAYED.name(), 1, 0);

        recalculateStanding(tournamentId, groupStageId, groupAId);
        recalculateStanding(tournamentId, groupStageId, groupBId);
        recalculateStanding(tournamentId, groupStageId, groupCId);
        recalculateStanding(tournamentId, groupStageId, groupDId);

        progressToKnockout(tournamentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetStageId").value(knockoutStageId))
                .andExpect(jsonPath("$.data.qualifiedTeamsCount").value(4));

        return new BracketScenario(
                tournamentId,
                knockoutStageId,
                tournamentTeamAId,
                tournamentTeamCId,
                tournamentTeamEId,
                tournamentTeamGId
        );
    }

    private long extractId(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("id").asLong();
    }

    private record BracketScenario(
            long tournamentId,
            long knockoutStageId,
            long qualifiedTeamAId,
            long qualifiedTeamCId,
            long qualifiedTeamEId,
            long qualifiedTeamGId
    ) {
    }
}
