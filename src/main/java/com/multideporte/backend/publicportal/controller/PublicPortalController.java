package com.multideporte.backend.publicportal.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.publicportal.dto.PublicPortalHomeResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentCalendarResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentDetailResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentResultsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentStandingsResponse;
import com.multideporte.backend.publicportal.dto.PublicTournamentSummaryResponse;
import com.multideporte.backend.publicportal.service.PublicPortalService;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicPortalController {

    private final PublicPortalService publicPortalService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse<PublicPortalHomeResponse>> getHome() {
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_PORTAL_HOME_FOUND",
                "Resumen publico obtenido correctamente",
                publicPortalService.getHome()
        ));
    }

    @GetMapping("/tournaments")
    public ResponseEntity<ApiResponse<PageResponse<PublicTournamentSummaryResponse>>> getTournaments(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) TournamentStatus status,
            @PageableDefault(size = 12, sort = "startDate") Pageable pageable
    ) {
        Page<PublicTournamentSummaryResponse> response = publicPortalService.getTournaments(name, sportId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_TOURNAMENT_PAGE",
                "Torneos publicos obtenidos correctamente",
                PageResponse.from(response)
        ));
    }

    @GetMapping("/tournaments/{slug}")
    public ResponseEntity<ApiResponse<PublicTournamentDetailResponse>> getTournamentDetail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_TOURNAMENT_FOUND",
                "Detalle publico de torneo obtenido correctamente",
                publicPortalService.getTournamentDetail(slug)
        ));
    }

    @GetMapping("/tournaments/{slug}/standings")
    public ResponseEntity<ApiResponse<PublicTournamentStandingsResponse>> getTournamentStandings(
            @PathVariable String slug,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_TOURNAMENT_STANDINGS_FOUND",
                "Standings publicos obtenidos correctamente",
                publicPortalService.getTournamentStandings(slug, stageId, groupId)
        ));
    }

    @GetMapping("/tournaments/{slug}/calendar")
    public ResponseEntity<ApiResponse<PublicTournamentCalendarResponse>> getTournamentCalendar(
            @PathVariable String slug,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) MatchGameStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_TOURNAMENT_CALENDAR_FOUND",
                "Calendario publico obtenido correctamente",
                publicPortalService.getTournamentCalendar(slug, stageId, groupId, status, from, to)
        ));
    }

    @GetMapping("/tournaments/{slug}/results")
    public ResponseEntity<ApiResponse<PublicTournamentResultsResponse>> getTournamentResults(
            @PathVariable String slug,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "PUBLIC_TOURNAMENT_RESULTS_FOUND",
                "Resultados publicos obtenidos correctamente",
                publicPortalService.getTournamentResults(slug, stageId, groupId)
        ));
    }
}
