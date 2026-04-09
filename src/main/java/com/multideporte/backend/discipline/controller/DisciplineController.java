package com.multideporte.backend.discipline.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.discipline.dto.request.DisciplinaryIncidentCreateRequest;
import com.multideporte.backend.discipline.dto.request.DisciplinarySanctionCreateRequest;
import com.multideporte.backend.discipline.dto.response.DisciplineMatchResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinaryIncidentResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionListResponse;
import com.multideporte.backend.discipline.dto.response.DisciplinarySanctionResponse;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionStatus;
import com.multideporte.backend.discipline.service.DisciplineService;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;
    private final OperationalAuditService operationalAuditService;

    @GetMapping("/matches/{matchId}/discipline")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<DisciplineMatchResponse>> getMatchDiscipline(@PathVariable Long matchId) {
        DisciplineMatchResponse response = disciplineService.getMatchDiscipline(matchId);
        return ResponseEntity.ok(ApiResponse.success(
                "DISCIPLINE_MATCH_FOUND",
                "Disciplina del partido obtenida correctamente",
                response
        ));
    }

    @PostMapping("/matches/{matchId}/discipline/incidents")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<DisciplinaryIncidentResponse>> createIncident(
            @PathVariable Long matchId,
            @Valid @RequestBody DisciplinaryIncidentCreateRequest request
    ) {
        DisciplinaryIncidentResponse response = disciplineService.createIncident(matchId, request);
        operationalAuditService.auditSuccess("DISCIPLINARY_INCIDENT_CREATE", "MATCH", matchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "DISCIPLINARY_INCIDENT_CREATED",
                        "Incidencia disciplinaria creada correctamente",
                        response
                ));
    }

    @PostMapping("/matches/{matchId}/discipline/incidents/{incidentId}/sanctions")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<DisciplinarySanctionResponse>> createSanction(
            @PathVariable Long matchId,
            @PathVariable Long incidentId,
            @Valid @RequestBody DisciplinarySanctionCreateRequest request
    ) {
        DisciplinarySanctionResponse response = disciplineService.createSanction(matchId, incidentId, request);
        operationalAuditService.auditSuccess("DISCIPLINARY_SANCTION_CREATE", "MATCH", matchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "DISCIPLINARY_SANCTION_CREATED",
                        "Sancion disciplinaria creada correctamente",
                        response
                ));
    }

    @GetMapping("/tournaments/{tournamentId}/discipline/sanctions")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<DisciplinarySanctionListResponse>> getTournamentSanctions(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) DisciplinarySanctionStatus status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Boolean activeOnly
    ) {
        DisciplinarySanctionListResponse response = disciplineService.getTournamentSanctions(
                tournamentId,
                status,
                teamId,
                playerId,
                matchId,
                activeOnly
        );
        return ResponseEntity.ok(ApiResponse.success(
                "DISCIPLINARY_SANCTIONS_FOUND",
                "Sanciones disciplinarias obtenidas correctamente",
                response
        ));
    }
}
