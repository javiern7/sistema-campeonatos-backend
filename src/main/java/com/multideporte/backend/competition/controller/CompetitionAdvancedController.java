package com.multideporte.backend.competition.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedBracketResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedCalendarResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedResultsResponse;
import com.multideporte.backend.competition.service.CompetitionAdvancedService;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.security.auth.SecurityPermissions;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/competition-advanced")
@RequiredArgsConstructor
public class CompetitionAdvancedController {

    private final CompetitionAdvancedService competitionAdvancedService;

    @GetMapping("/bracket")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<CompetitionAdvancedBracketResponse>> getBracket(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long stageId
    ) {
        CompetitionAdvancedBracketResponse response = competitionAdvancedService.getBracket(tournamentId, stageId);
        return ResponseEntity.ok(ApiResponse.success(
                "COMPETITION_ADVANCED_BRACKET_FOUND",
                "Bracket de competencia avanzada obtenido correctamente",
                response
        ));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<CompetitionAdvancedCalendarResponse>> getCalendar(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) MatchGameStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        CompetitionAdvancedCalendarResponse response = competitionAdvancedService.getCalendar(
                tournamentId,
                stageId,
                groupId,
                status,
                from,
                to
        );
        return ResponseEntity.ok(ApiResponse.success(
                "COMPETITION_ADVANCED_CALENDAR_FOUND",
                "Calendario de competencia avanzada obtenido correctamente",
                response
        ));
    }

    @GetMapping("/results")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<CompetitionAdvancedResultsResponse>> getResults(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId
    ) {
        CompetitionAdvancedResultsResponse response = competitionAdvancedService.getResults(tournamentId, stageId, groupId);
        return ResponseEntity.ok(ApiResponse.success(
                "COMPETITION_ADVANCED_RESULTS_FOUND",
                "Resultados de competencia avanzada obtenidos correctamente",
                response
        ));
    }
}
