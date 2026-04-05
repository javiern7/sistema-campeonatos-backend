package com.multideporte.backend.team.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.team.dto.request.TeamCreateRequest;
import com.multideporte.backend.team.dto.request.TeamUpdateRequest;
import com.multideporte.backend.team.dto.response.TeamResponse;
import com.multideporte.backend.team.service.TeamService;
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
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final OperationalAuditService operationalAuditService;

    @PostMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TEAMS)
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody TeamCreateRequest request) {
        TeamResponse response = teamService.create(request);
        operationalAuditService.auditSuccess("TEAM_CREATE", "TEAM", response.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("TEAM_CREATED", "Equipo creado correctamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TEAMS_READ + "')")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable Long id) {
        TeamResponse response = teamService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("TEAM_FOUND", "Equipo obtenido correctamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TEAMS_READ + "')")
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        Page<TeamResponse> response = teamService.getAll(name, code, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("TEAM_PAGE", "Equipos obtenidos correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TEAMS)
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamUpdateRequest request
    ) {
        TeamResponse response = teamService.update(id, request);
        operationalAuditService.auditSuccess("TEAM_UPDATE", "TEAM", id);
        return ResponseEntity.ok(ApiResponse.success("TEAM_UPDATED", "Equipo actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_DELETE_TEAMS)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        operationalAuditService.auditSuccess("TEAM_DELETE", "TEAM", id);
        return ResponseEntity.ok(ApiResponse.success("TEAM_DELETED", "Equipo eliminado correctamente"));
    }
}
