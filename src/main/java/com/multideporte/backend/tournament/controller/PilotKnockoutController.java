package com.multideporte.backend.tournament.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.tournament.dto.response.PilotKnockoutResponse;
import com.multideporte.backend.tournament.dto.response.PilotFinalClassificationResponse;
import com.multideporte.backend.tournament.service.PilotKnockoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/p56")
@RequiredArgsConstructor
public class PilotKnockoutController {
    private final PilotKnockoutService knockoutService;
    private final OperationalAuditService auditService;

    @PostMapping("/group-stages/{groupStageId}/knockout-stages/{knockoutStageId}/semifinals")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<PilotKnockoutResponse>> semifinals(@PathVariable Long tournamentId,
            @PathVariable Long groupStageId, @PathVariable Long knockoutStageId) {
        PilotKnockoutResponse response = knockoutService.createSemifinals(tournamentId, groupStageId, knockoutStageId);
        auditService.auditSuccess("P56_SEMIFINALS_CREATE", "TOURNAMENT", tournamentId);
        return ResponseEntity.ok(ApiResponse.success("P56_SEMIFINALS_READY", "Semifinales P56 creadas", response));
    }

    @GetMapping("/knockout-stages/{knockoutStageId}/final-classification")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<PilotFinalClassificationResponse>> classification(@PathVariable Long tournamentId,
            @PathVariable Long knockoutStageId) {
        return ResponseEntity.ok(ApiResponse.success("P56_FINAL_CLASSIFICATION", "Clasificacion final P56 obtenida",
                knockoutService.finalClassification(tournamentId, knockoutStageId)));
    }
    @PostMapping("/knockout-stages/{knockoutStageId}/final-round")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<PilotKnockoutResponse>> finalRound(@PathVariable Long tournamentId,
            @PathVariable Long knockoutStageId) {
        PilotKnockoutResponse response = knockoutService.createFinalAndThirdPlace(tournamentId, knockoutStageId);
        auditService.auditSuccess("P56_FINAL_ROUND_CREATE", "TOURNAMENT", tournamentId);
        return ResponseEntity.ok(ApiResponse.success("P56_FINAL_ROUND_READY", "Final y tercer puesto P56 creados", response));
    }
}
