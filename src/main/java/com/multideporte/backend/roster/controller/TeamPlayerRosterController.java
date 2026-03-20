package com.multideporte.backend.roster.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterCreateRequest;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterUpdateRequest;
import com.multideporte.backend.roster.dto.response.TeamPlayerRosterResponse;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.service.TeamPlayerRosterService;
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
@RequestMapping("/rosters")
@RequiredArgsConstructor
public class TeamPlayerRosterController {

    private final TeamPlayerRosterService teamPlayerRosterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TeamPlayerRosterResponse>> create(
            @Valid @RequestBody TeamPlayerRosterCreateRequest request
    ) {
        TeamPlayerRosterResponse response = teamPlayerRosterService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("ROSTER_CREATED", "Registro de roster creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamPlayerRosterResponse>> getById(@PathVariable Long id) {
        TeamPlayerRosterResponse response = teamPlayerRosterService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("ROSTER_FOUND", "Registro de roster obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TeamPlayerRosterResponse>>> getAll(
            @RequestParam(required = false) Long tournamentTeamId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) RosterStatus rosterStatus,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<TeamPlayerRosterResponse> response = teamPlayerRosterService.getAll(tournamentTeamId, playerId, rosterStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success("ROSTER_PAGE", "Registros de roster obtenidos correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<TeamPlayerRosterResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamPlayerRosterUpdateRequest request
    ) {
        TeamPlayerRosterResponse response = teamPlayerRosterService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("ROSTER_UPDATED", "Registro de roster actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamPlayerRosterService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("ROSTER_DELETED", "Registro de roster eliminado correctamente"));
    }
}
