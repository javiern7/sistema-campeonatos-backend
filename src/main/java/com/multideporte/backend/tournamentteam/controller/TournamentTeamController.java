package com.multideporte.backend.tournamentteam.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamCreateRequest;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamUpdateRequest;
import com.multideporte.backend.tournamentteam.dto.response.TournamentTeamResponse;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.service.TournamentTeamService;
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
@RequestMapping("/tournament-teams")
@RequiredArgsConstructor
public class TournamentTeamController {

    private final TournamentTeamService tournamentTeamService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TournamentTeamResponse>> create(
            @Valid @RequestBody TournamentTeamCreateRequest request
    ) {
        TournamentTeamResponse response = tournamentTeamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TOURNAMENT_TEAM_CREATED", "Inscripcion creada correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentTeamResponse>> getById(@PathVariable Long id) {
        TournamentTeamResponse response = tournamentTeamService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_TEAM_FOUND", "Inscripcion obtenida correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TournamentTeamResponse>>> getAll(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) TournamentTeamRegistrationStatus registrationStatus,
            @PageableDefault(size = 20, sort = {"seedNumber", "id"}) Pageable pageable
    ) {
        Page<TournamentTeamResponse> response = tournamentTeamService.getAll(tournamentId, teamId, registrationStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_TEAM_PAGE", "Inscripciones obtenidas correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TournamentTeamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TournamentTeamUpdateRequest request
    ) {
        TournamentTeamResponse response = tournamentTeamService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_TEAM_UPDATED", "Inscripcion actualizada correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tournamentTeamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("TOURNAMENT_TEAM_DELETED", "Inscripcion eliminada correctamente"));
    }
}
