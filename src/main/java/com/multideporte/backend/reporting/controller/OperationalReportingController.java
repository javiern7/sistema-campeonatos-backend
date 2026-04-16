package com.multideporte.backend.reporting.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.reporting.dto.CardReportRow;
import com.multideporte.backend.reporting.dto.EventReportRow;
import com.multideporte.backend.reporting.dto.MatchReportRow;
import com.multideporte.backend.reporting.dto.OperationalReportResponse;
import com.multideporte.backend.reporting.dto.ReportExportResponse;
import com.multideporte.backend.reporting.dto.ScorerReportRow;
import com.multideporte.backend.reporting.dto.StandingReportRow;
import com.multideporte.backend.reporting.dto.TournamentSummaryReportRow;
import com.multideporte.backend.reporting.service.OperationalReportingService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/reports")
@RequiredArgsConstructor
public class OperationalReportingController {

    private final OperationalReportingService operationalReportingService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<TournamentSummaryReportRow>>> getSummary(
            @PathVariable Long tournamentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_TOURNAMENT_SUMMARY_FOUND",
                "Reporte resumen de torneo obtenido correctamente",
                operationalReportingService.getTournamentSummary(tournamentId)
        ));
    }

    @GetMapping("/matches")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<MatchReportRow>>> getMatches(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) MatchGameStatus status,
            @RequestParam(required = false) OffsetDateTime scheduledFrom,
            @RequestParam(required = false) OffsetDateTime scheduledTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_MATCHES_FOUND",
                "Reporte de partidos obtenido correctamente",
                operationalReportingService.getMatchesReport(tournamentId, tournamentTeamId, teamId, status, scheduledFrom, scheduledTo)
        ));
    }

    @GetMapping("/standings")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.STANDINGS_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<StandingReportRow>>> getStandings(
            @PathVariable Long tournamentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_STANDINGS_FOUND",
                "Reporte de standings obtenido correctamente",
                operationalReportingService.getStandingsReport(tournamentId)
        ));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<EventReportRow>>> getEvents(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) OffsetDateTime scheduledFrom,
            @RequestParam(required = false) OffsetDateTime scheduledTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_EVENTS_FOUND",
                "Reporte de eventos obtenido correctamente",
                operationalReportingService.getEventsReport(tournamentId, matchId, tournamentTeamId, teamId, playerId, scheduledFrom, scheduledTo)
        ));
    }

    @GetMapping("/scorers")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<ScorerReportRow>>> getScorers(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_SCORERS_FOUND",
                "Reporte de goleadores obtenido correctamente",
                operationalReportingService.getScorersReport(tournamentId, tournamentTeamId, teamId)
        ));
    }

    @GetMapping("/cards")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<OperationalReportResponse<CardReportRow>>> getCards(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "REPORT_CARDS_FOUND",
                "Reporte de tarjetas obtenido correctamente",
                operationalReportingService.getCardsReport(tournamentId, tournamentTeamId, teamId)
        ));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "','" + SecurityPermissions.MATCHES_READ + "','" + SecurityPermissions.STANDINGS_READ + "')")
    public ResponseEntity<byte[]> export(
            @PathVariable Long tournamentId,
            @RequestParam String reportType,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) MatchGameStatus status,
            @RequestParam(required = false) OffsetDateTime scheduledFrom,
            @RequestParam(required = false) OffsetDateTime scheduledTo
    ) {
        ReportExportResponse export = operationalReportingService.exportFile(
                tournamentId, reportType, format, matchId, tournamentTeamId, teamId, playerId, status, scheduledFrom, scheduledTo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(export.fileName()).build().toString())
                .body(export.content());
    }
}
