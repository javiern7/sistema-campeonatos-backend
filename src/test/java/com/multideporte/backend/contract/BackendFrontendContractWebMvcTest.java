package com.multideporte.backend.contract;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multideporte.backend.common.exception.GlobalExceptionHandler;
import com.multideporte.backend.match.controller.MatchGameController;
import com.multideporte.backend.match.dto.response.MatchGameResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.service.MatchGameService;
import com.multideporte.backend.roster.controller.TeamPlayerRosterController;
import com.multideporte.backend.roster.dto.response.TeamPlayerRosterResponse;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.service.TeamPlayerRosterService;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.stage.controller.TournamentStageController;
import com.multideporte.backend.stage.dto.response.TournamentStageResponse;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.service.TournamentStageService;
import com.multideporte.backend.stagegroup.controller.StageGroupController;
import com.multideporte.backend.stagegroup.dto.response.StageGroupResponse;
import com.multideporte.backend.stagegroup.service.StageGroupService;
import com.multideporte.backend.standing.controller.StandingController;
import com.multideporte.backend.standing.dto.response.StandingResponse;
import com.multideporte.backend.standing.service.StandingService;
import com.multideporte.backend.team.controller.TeamController;
import com.multideporte.backend.team.dto.response.TeamResponse;
import com.multideporte.backend.team.service.TeamService;
import com.multideporte.backend.tournament.controller.TournamentController;
import com.multideporte.backend.tournament.service.TournamentService;
import com.multideporte.backend.tournamentteam.controller.TournamentTeamController;
import com.multideporte.backend.tournamentteam.dto.response.TournamentTeamResponse;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.service.TournamentTeamService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BackendFrontendContractWebMvcTest {

    @Mock
    private TeamService teamService;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private TournamentTeamService tournamentTeamService;

    @Mock
    private TournamentStageService tournamentStageService;

    @Mock
    private StageGroupService stageGroupService;

    @Mock
    private TeamPlayerRosterService teamPlayerRosterService;

    @Mock
    private MatchGameService matchGameService;

    @Mock
    private StandingService standingService;

    @Mock
    private OperationalAuditService operationalAuditService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(
                        new TeamController(teamService, operationalAuditService),
                        new TournamentController(tournamentService, operationalAuditService),
                        new TournamentTeamController(tournamentTeamService, operationalAuditService),
                        new TournamentStageController(tournamentStageService, operationalAuditService),
                        new StageGroupController(stageGroupService, operationalAuditService),
                        new TeamPlayerRosterController(teamPlayerRosterService, operationalAuditService),
                        new MatchGameController(matchGameService, operationalAuditService),
                        new StandingController(standingService, operationalAuditService)
                )
                .setControllerAdvice(new GlobalExceptionHandler(operationalAuditService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void shouldExposeStablePageResponseForTeamsAndUseFriendlyDefaultSort() throws Exception {
        TeamResponse team = new TeamResponse(1L, "Alpha FC", "Alpha", "ALPHA", "#000000", "#FFFFFF", true, null, null);
        when(teamService.getAll(eq(null), eq(null), eq(null), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(team),
                        PageRequest.of(0, 20, Sort.by("name")),
                        1
                ));

        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Alpha FC"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.sort[0].property").value("name"))
                .andExpect(jsonPath("$.data.sort[0].direction").value("ASC"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(teamService).getAll(eq(null), eq(null), eq(null), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("name");
        org.junit.jupiter.api.Assertions.assertNotNull(order);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldReturnClearErrorWhenQueryEnumIsInvalid() throws Exception {
        mockMvc.perform(get("/tournament-teams")
                        .param("registrationStatus", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Parametro invalido: registrationStatus"));
    }

    @Test
    void shouldExposeStablePageResponseForOperationalCrudModules() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-11T10:00:00Z");
        when(tournamentStageService.getAll(eq(1L), eq(TournamentStageType.GROUP_STAGE), eq(true), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new TournamentStageResponse(2L, 1L, "Fase de grupos", TournamentStageType.GROUP_STAGE, 1, 1, false, true, now)),
                        PageRequest.of(0, 20, Sort.by("sequenceOrder")),
                        1
                ));
        when(stageGroupService.getAll(eq(2L), eq("A"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new StageGroupResponse(3L, 2L, "A", "Grupo A", 1, now)),
                        PageRequest.of(0, 20, Sort.by("sequenceOrder")),
                        1
                ));
        when(teamPlayerRosterService.getAll(eq(4L), eq(5L), eq(RosterStatus.ACTIVE), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new TeamPlayerRosterResponse(6L, 4L, 5L, 9, false, "Delantero", RosterStatus.ACTIVE, LocalDate.parse("2026-04-01"), null, now)),
                        PageRequest.of(0, 20, Sort.by("id")),
                        1
                ));
        when(matchGameService.getAll(eq(1L), eq(2L), eq(3L), eq(MatchGameStatus.SCHEDULED), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new MatchGameResponse(7L, 1L, 2L, 3L, 1, 1, 10L, 11L, now, "Cancha 1", MatchGameStatus.SCHEDULED, null, null, null, null, now, now)),
                        PageRequest.of(0, 20, Sort.by("roundNumber", "matchdayNumber", "id")),
                        1
                ));
        when(standingService.getAll(eq(1L), eq(2L), eq(3L), eq(4L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new StandingResponse(8L, 1L, 2L, 3L, 4L, 1, 1, 0, 0, 2, 0, 2, 3, 1, now)),
                        PageRequest.of(0, 20, Sort.by("rankPosition")),
                        1
                ));

        mockMvc.perform(get("/tournament-stages")
                        .param("tournamentId", "1")
                        .param("stageType", "GROUP_STAGE")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(2))
                .andExpect(jsonPath("$.data.content[0].stageType").value("GROUP_STAGE"))
                .andExpect(jsonPath("$.data.sort[0].property").value("sequenceOrder"));

        mockMvc.perform(get("/stage-groups")
                        .param("stageId", "2")
                        .param("code", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].code").value("A"))
                .andExpect(jsonPath("$.data.sort[0].property").value("sequenceOrder"));

        mockMvc.perform(get("/rosters")
                        .param("tournamentTeamId", "4")
                        .param("playerId", "5")
                        .param("rosterStatus", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rosterStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.sort[0].property").value("id"));

        mockMvc.perform(get("/matches")
                        .param("tournamentId", "1")
                        .param("stageId", "2")
                        .param("groupId", "3")
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.sort[0].property").value("roundNumber"));

        mockMvc.perform(get("/standings")
                        .param("tournamentId", "1")
                        .param("stageId", "2")
                        .param("groupId", "3")
                        .param("tournamentTeamId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rankPosition").value(1))
                .andExpect(jsonPath("$.data.sort[0].property").value("rankPosition"));
    }

    @Test
    void shouldMapCreatedAtSortAliasForCrudOperationalModulesWithoutCreatedAtField() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-11T10:00:00Z");
        when(tournamentTeamService.getAll(eq(null), eq(null), eq(null), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new TournamentTeamResponse(9L, 1L, 2L, TournamentTeamRegistrationStatus.APPROVED, 1, 1, now)),
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "joinedAt")),
                        1
                ));
        when(standingService.getAll(eq(null), eq(null), eq(null), eq(null), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(new StandingResponse(10L, 1L, null, null, 9L, 0, 0, 0, 0, 0, 0, 0, 0, null, now)),
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt")),
                        1
                ));

        mockMvc.perform(get("/tournament-teams")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sort[0].property").value("joinedAt"))
                .andExpect(jsonPath("$.data.sort[0].direction").value("DESC"));

        mockMvc.perform(get("/standings")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sort[0].property").value("updatedAt"))
                .andExpect(jsonPath("$.data.sort[0].direction").value("DESC"));

        ArgumentCaptor<Pageable> tournamentTeamsPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(tournamentTeamService).getAll(eq(null), eq(null), eq(null), tournamentTeamsPageableCaptor.capture());
        Sort.Order tournamentTeamOrder = tournamentTeamsPageableCaptor.getValue().getSort().getOrderFor("joinedAt");
        org.junit.jupiter.api.Assertions.assertNotNull(tournamentTeamOrder);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, tournamentTeamOrder.getDirection());

        ArgumentCaptor<Pageable> standingsPageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(standingService).getAll(eq(null), eq(null), eq(null), eq(null), standingsPageableCaptor.capture());
        Sort.Order standingOrder = standingsPageableCaptor.getValue().getSort().getOrderFor("updatedAt");
        org.junit.jupiter.api.Assertions.assertNotNull(standingOrder);
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, standingOrder.getDirection());
    }

    @Test
    void shouldReturnClearErrorWhenBodyEnumIsInvalid() throws Exception {
        mockMvc.perform(post("/tournaments/{id}/status-transition", 10L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("targetStatus", "NOT_A_STATUS"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request JSON invalido o con valores no soportados"));
    }
}
