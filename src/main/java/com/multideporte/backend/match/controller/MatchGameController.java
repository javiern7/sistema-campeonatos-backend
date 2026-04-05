package com.multideporte.backend.match.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.match.dto.request.MatchGameCreateRequest;
import com.multideporte.backend.match.dto.request.MatchGameUpdateRequest;
import com.multideporte.backend.match.dto.response.MatchGameResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.service.MatchGameService;
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
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchGameController {

    private final MatchGameService matchGameService;
    private final OperationalAuditService operationalAuditService;

    @PostMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<MatchGameResponse>> create(@Valid @RequestBody MatchGameCreateRequest request) {
        MatchGameResponse response = matchGameService.create(request);
        operationalAuditService.auditSuccess("MATCH_CREATE", "MATCH", response.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("MATCH_CREATED", "Partido creado correctamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<MatchGameResponse>> getById(@PathVariable Long id) {
        MatchGameResponse response = matchGameService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("MATCH_FOUND", "Partido obtenido correctamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<PageResponse<MatchGameResponse>>> getAll(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) MatchGameStatus status,
            @PageableDefault(size = 20, sort = {"roundNumber", "matchdayNumber", "id"}) Pageable pageable
    ) {
        Page<MatchGameResponse> response = matchGameService.getAll(tournamentId, stageId, groupId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("MATCH_PAGE", "Partidos obtenidos correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<MatchGameResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MatchGameUpdateRequest request
    ) {
        MatchGameResponse response = matchGameService.update(id, request);
        operationalAuditService.auditSuccess("MATCH_UPDATE", "MATCH", id);
        return ResponseEntity.ok(ApiResponse.success("MATCH_UPDATED", "Partido actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_DELETE_MATCHES)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        matchGameService.delete(id);
        operationalAuditService.auditSuccess("MATCH_DELETE", "MATCH", id);
        return ResponseEntity.ok(ApiResponse.success("MATCH_DELETED", "Partido eliminado correctamente"));
    }
}
