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
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.team.controller.TeamController;
import com.multideporte.backend.team.dto.response.TeamResponse;
import com.multideporte.backend.team.service.TeamService;
import com.multideporte.backend.tournament.controller.TournamentController;
import com.multideporte.backend.tournament.service.TournamentService;
import com.multideporte.backend.tournamentteam.controller.TournamentTeamController;
import com.multideporte.backend.tournamentteam.service.TournamentTeamService;
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
    private OperationalAuditService operationalAuditService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new TeamController(teamService, operationalAuditService),
                        new TournamentController(tournamentService, operationalAuditService),
                        new TournamentTeamController(tournamentTeamService, operationalAuditService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
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
