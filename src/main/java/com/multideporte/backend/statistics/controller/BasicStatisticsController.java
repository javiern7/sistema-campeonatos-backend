package com.multideporte.backend.statistics.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsResponse;
import com.multideporte.backend.statistics.dto.response.EventStatisticsResponse;
import com.multideporte.backend.statistics.service.BasicStatisticsService;
import com.multideporte.backend.statistics.service.EventStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/statistics")
@RequiredArgsConstructor
public class BasicStatisticsController {

    private final BasicStatisticsService basicStatisticsService;
    private final EventStatisticsService eventStatisticsService;

    @GetMapping("/basic")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<BasicStatisticsResponse>> getBasicStatistics(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId
    ) {
        BasicStatisticsResponse response = basicStatisticsService.getBasicStatistics(tournamentId, stageId, groupId);
        return ResponseEntity.ok(ApiResponse.success(
                "BASIC_STATISTICS_FOUND",
                "Estadisticas basicas obtenidas correctamente",
                response
        ));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<EventStatisticsResponse>> getEventStatistics(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId
    ) {
        EventStatisticsResponse response = eventStatisticsService.getEventStatistics(
                tournamentId,
                matchId,
                tournamentTeamId,
                teamId,
                playerId
        );
        return ResponseEntity.ok(ApiResponse.success(
                "EVENT_STATISTICS_FOUND",
                "Estadisticas derivadas de eventos obtenidas correctamente",
                response
        ));
    }
}
