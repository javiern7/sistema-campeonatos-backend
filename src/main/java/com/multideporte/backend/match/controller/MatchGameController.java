package com.multideporte.backend.match.controller;

import com.multideporte.backend.common.api.ApiResponse;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<MatchGameResponse>> create(@Valid @RequestBody MatchGameCreateRequest request) {
        MatchGameResponse response = matchGameService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("MATCH_CREATED", "Partido creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchGameResponse>> getById(@PathVariable Long id) {
        MatchGameResponse response = matchGameService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("MATCH_FOUND", "Partido obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MatchGameResponse>>> getAll(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) MatchGameStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<MatchGameResponse> response = matchGameService.getAll(tournamentId, stageId, groupId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("MATCH_PAGE", "Partidos obtenidos correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<MatchGameResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MatchGameUpdateRequest request
    ) {
        MatchGameResponse response = matchGameService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("MATCH_UPDATED", "Partido actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        matchGameService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("MATCH_DELETED", "Partido eliminado correctamente"));
    }
}
