package com.multideporte.backend.tournament.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.tournament.dto.request.TournamentKnockoutBracketGenerateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentStatusTransitionRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutBracketResponse;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.dto.response.TournamentOperationalSummaryResponse;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final OperationalAuditService operationalAuditService;

    @PostMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<TournamentResponse>> create(@Valid @RequestBody TournamentCreateRequest request) {
        TournamentResponse response = tournamentService.create(request);
        operationalAuditService.auditSuccess("TOURNAMENT_CREATE", "TOURNAMENT", response.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TOURNAMENT_CREATED", "Torneo creado correctamente", response));
    }

    @GetMapping("/operational-summary")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<PageResponse<TournamentOperationalSummaryResponse>>> getOperationalSummaries(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) TournamentOperationalCategory operationalCategory,
            @RequestParam(required = false) Boolean executiveOnly,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        Page<TournamentOperationalSummaryResponse> response = tournamentService.getOperationalSummaries(
                name,
                sportId,
                status,
                operationalCategory,
                executiveOnly,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(
                "TOURNAMENT_OPERATIONAL_SUMMARY_PAGE",
                "Resumenes operativos obtenidos correctamente",
                PageResponse.from(response)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<TournamentResponse>> getById(@PathVariable Long id) {
        TournamentResponse response = tournamentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_FOUND", "Torneo obtenido correctamente", response));
    }

    @GetMapping("/{id}/operational-summary")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<TournamentOperationalSummaryResponse>> getOperationalSummaryById(@PathVariable Long id) {
        TournamentOperationalSummaryResponse response = tournamentService.getOperationalSummaryById(id);
        return ResponseEntity.ok(ApiResponse.success(
                "TOURNAMENT_OPERATIONAL_SUMMARY_FOUND",
                "Resumen operativo de torneo obtenido correctamente",
                response
        ));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<PageResponse<TournamentResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) TournamentOperationalCategory operationalCategory,
            @RequestParam(required = false) Boolean executiveOnly,
            @PageableDefault(size = 20, sort = "startDate") Pageable pageable
    ) {
        Page<TournamentResponse> response = tournamentService.getAll(
                name,
                sportId,
                status,
                operationalCategory,
                executiveOnly,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_PAGE", "Torneos obtenidos correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<TournamentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentUpdateRequest request
    ) {
        TournamentResponse response = tournamentService.update(id, request);
        operationalAuditService.auditSuccess("TOURNAMENT_UPDATE", "TOURNAMENT", id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_UPDATED", "Torneo actualizado correctamente", response));
    }

    @PostMapping("/{id}/status-transition")
    @PreAuthorize(SecurityPermissions.CAN_TRANSITION_TOURNAMENT_STATUS)
    public ResponseEntity<ApiResponse<TournamentResponse>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody TournamentStatusTransitionRequest request
    ) {
        TournamentResponse response = tournamentService.transitionStatus(id, request);
        operationalAuditService.auditSuccess("TOURNAMENT_STATUS_TRANSITION", "TOURNAMENT", id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_STATUS_CHANGED", "Estado de torneo actualizado correctamente", response));
    }

    @PostMapping("/{id}/progress-to-knockout")
    @PreAuthorize(SecurityPermissions.CAN_PROGRESS_TOURNAMENT_TO_KNOCKOUT)
    public ResponseEntity<ApiResponse<TournamentKnockoutProgressionResponse>> progressToKnockout(@PathVariable Long id) {
        TournamentKnockoutProgressionResponse response = tournamentService.progressToKnockout(id);
        operationalAuditService.auditSuccess("TOURNAMENT_PROGRESS_TO_KNOCKOUT", "TOURNAMENT", id);
        return ResponseEntity.ok(ApiResponse.success(
                "TOURNAMENT_KNOCKOUT_READY",
                "Progresion a fase eliminatoria realizada correctamente",
                response
        ));
    }

    @PostMapping("/{id}/generate-knockout-bracket")
    @PreAuthorize(SecurityPermissions.CAN_GENERATE_TOURNAMENT_KNOCKOUT_BRACKET)
    public ResponseEntity<ApiResponse<TournamentKnockoutBracketResponse>> generateKnockoutBracket(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TournamentKnockoutBracketGenerateRequest request
    ) {
        TournamentKnockoutBracketResponse response = tournamentService.generateKnockoutBracket(id, request);
        operationalAuditService.auditSuccess("TOURNAMENT_GENERATE_KNOCKOUT_BRACKET", "TOURNAMENT", id);
        return ResponseEntity.ok(ApiResponse.success(
                "TOURNAMENT_KNOCKOUT_BRACKET_GENERATED",
                "Bracket eliminatorio inicial generado correctamente",
                response
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_DELETE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tournamentService.delete(id);
        operationalAuditService.auditSuccess("TOURNAMENT_DELETE", "TOURNAMENT", id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_DELETED", "Torneo eliminado correctamente"));
    }
}
