package com.multideporte.backend.tournament.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.tournament.dto.request.PilotGroupAssignmentRequest;
import com.multideporte.backend.tournament.dto.response.PilotFixtureResponse;
import com.multideporte.backend.tournament.service.PilotFixtureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/p56/stages/{stageId}")
@RequiredArgsConstructor
public class PilotFixtureController {
    private final PilotFixtureService fixtureService;
    private final OperationalAuditService auditService;

    @PostMapping("/groups/{groupId}/teams")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<Void>> assignGroup(@PathVariable Long tournamentId, @PathVariable Long stageId,
                                                           @PathVariable Long groupId,
                                                           @Valid @RequestBody PilotGroupAssignmentRequest request) {
        fixtureService.assignGroup(tournamentId, stageId, groupId, request);
        auditService.auditSuccess("P56_GROUP_ASSIGNMENT", "TOURNAMENT_STAGE", stageId);
        return ResponseEntity.ok(ApiResponse.success("P56_GROUP_ASSIGNED", "Grupo del piloto asignado"));
    }

    @PostMapping("/fixture")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<PilotFixtureResponse>> generate(@PathVariable Long tournamentId, @PathVariable Long stageId) {
        PilotFixtureResponse response = fixtureService.generate(tournamentId, stageId);
        auditService.auditSuccess("P56_FIXTURE_GENERATE", "TOURNAMENT_STAGE", stageId);
        return ResponseEntity.ok(ApiResponse.success("P56_FIXTURE_READY", "Fixture oficial P56 generado", response));
    }
}
