package com.multideporte.backend.statistics.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsResponse;
import com.multideporte.backend.statistics.service.BasicStatisticsService;
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
}
