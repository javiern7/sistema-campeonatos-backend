package com.multideporte.backend.tournament.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> create(@Valid @RequestBody TournamentCreateRequest request) {
        TournamentResponse response = tournamentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TOURNAMENT_CREATED", "Torneo creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentResponse>> getById(@PathVariable Long id) {
        TournamentResponse response = tournamentService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_FOUND", "Torneo obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) TournamentStatus status,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<TournamentResponse> response = tournamentService.getAll(name, sportId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_PAGE", "Torneos obtenidos correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TournamentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentUpdateRequest request
    ) {
        TournamentResponse response = tournamentService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_UPDATED", "Torneo actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tournamentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_DELETED", "Torneo eliminado correctamente"));
    }
}
