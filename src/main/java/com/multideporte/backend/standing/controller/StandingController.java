package com.multideporte.backend.standing.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.standing.dto.request.StandingCreateRequest;
import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.request.StandingUpdateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;
import com.multideporte.backend.standing.dto.response.StandingResponse;
import com.multideporte.backend.standing.service.StandingService;
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
@RequestMapping("/standings")
@RequiredArgsConstructor
public class StandingController {

    private final StandingService standingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<StandingResponse>> create(@Valid @RequestBody StandingCreateRequest request) {
        StandingResponse response = standingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("STANDING_CREATED", "Standing creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StandingResponse>> getById(@PathVariable Long id) {
        StandingResponse response = standingService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("STANDING_FOUND", "Standing obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StandingResponse>>> getAll(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long tournamentTeamId,
            @PageableDefault(size = 20, sort = "rankPosition") Pageable pageable
    ) {
        Page<StandingResponse> response = standingService.getAll(tournamentId, stageId, groupId, tournamentTeamId, pageable);
        return ResponseEntity.ok(ApiResponse.success("STANDING_PAGE", "Standings obtenidos correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<StandingResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody StandingUpdateRequest request
    ) {
        StandingResponse response = standingService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("STANDING_UPDATED", "Standing actualizado correctamente", response));
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<StandingRecalculationResponse>> recalculate(
            @Valid @RequestBody StandingRecalculateRequest request
    ) {
        StandingRecalculationResponse response = standingService.recalculate(request);
        return ResponseEntity.ok(ApiResponse.success("STANDING_RECALCULATED", "Standings recalculados correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        standingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("STANDING_DELETED", "Standing eliminado correctamente"));
    }
}
