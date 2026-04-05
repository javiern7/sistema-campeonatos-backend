package com.multideporte.backend.stage.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.stage.dto.request.TournamentStageCreateRequest;
import com.multideporte.backend.stage.dto.request.TournamentStageUpdateRequest;
import com.multideporte.backend.stage.dto.response.TournamentStageResponse;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.service.TournamentStageService;
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
@RequestMapping("/tournament-stages")
@RequiredArgsConstructor
public class TournamentStageController {

    private final TournamentStageService tournamentStageService;
    private final OperationalAuditService operationalAuditService;

    @PostMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENT_STAGES)
    public ResponseEntity<ApiResponse<TournamentStageResponse>> create(@Valid @RequestBody TournamentStageCreateRequest request) {
        TournamentStageResponse response = tournamentStageService.create(request);
        operationalAuditService.auditSuccess("TOURNAMENT_STAGE_CREATE", "TOURNAMENT_STAGE", response.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TOURNAMENT_STAGE_CREATED", "Etapa creada correctamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENT_STAGES_READ + "')")
    public ResponseEntity<ApiResponse<TournamentStageResponse>> getById(@PathVariable Long id) {
        TournamentStageResponse response = tournamentStageService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_STAGE_FOUND", "Etapa obtenida correctamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENT_STAGES_READ + "')")
    public ResponseEntity<ApiResponse<PageResponse<TournamentStageResponse>>> getAll(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) TournamentStageType stageType,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "sequenceOrder") Pageable pageable
    ) {
        Page<TournamentStageResponse> response = tournamentStageService.getAll(tournamentId, stageType, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_STAGE_PAGE", "Etapas obtenidas correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENT_STAGES)
    public ResponseEntity<ApiResponse<TournamentStageResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentStageUpdateRequest request
    ) {
        TournamentStageResponse response = tournamentStageService.update(id, request);
        operationalAuditService.auditSuccess("TOURNAMENT_STAGE_UPDATE", "TOURNAMENT_STAGE", id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_STAGE_UPDATED", "Etapa actualizada correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_DELETE_TOURNAMENT_STAGES)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tournamentStageService.delete(id);
        operationalAuditService.auditSuccess("TOURNAMENT_STAGE_DELETE", "TOURNAMENT_STAGE", id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_STAGE_DELETED", "Etapa eliminada correctamente"));
    }
}
