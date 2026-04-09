package com.multideporte.backend.finance.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.finance.dto.request.FinancialMovementCreateRequest;
import com.multideporte.backend.finance.dto.response.BasicFinancialSummaryResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementListResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementResponse;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import com.multideporte.backend.finance.service.BasicFinanceService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments/{tournamentId}/finances")
@RequiredArgsConstructor
public class BasicFinanceController {

    private final BasicFinanceService basicFinanceService;
    private final OperationalAuditService operationalAuditService;

    @PostMapping("/movements")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_TOURNAMENTS)
    public ResponseEntity<ApiResponse<FinancialMovementResponse>> createMovement(
            @PathVariable Long tournamentId,
            @Valid @RequestBody FinancialMovementCreateRequest request
    ) {
        FinancialMovementResponse response = basicFinanceService.createMovement(tournamentId, request);
        operationalAuditService.auditSuccess("FINANCIAL_MOVEMENT_CREATE", "TOURNAMENT", tournamentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "FINANCIAL_MOVEMENT_CREATED",
                        "Movimiento financiero creado correctamente",
                        response
                ));
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<FinancialMovementListResponse>> getMovements(
            @PathVariable Long tournamentId,
            @RequestParam(required = false) FinancialMovementType movementType,
            @RequestParam(required = false) FinancialMovementCategory category,
            @RequestParam(required = false) Long tournamentTeamId
    ) {
        FinancialMovementListResponse response = basicFinanceService.getMovements(
                tournamentId,
                movementType,
                category,
                tournamentTeamId
        );
        return ResponseEntity.ok(ApiResponse.success(
                "FINANCIAL_MOVEMENTS_FOUND",
                "Movimientos financieros obtenidos correctamente",
                response
        ));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.TOURNAMENTS_READ + "')")
    public ResponseEntity<ApiResponse<BasicFinancialSummaryResponse>> getSummary(@PathVariable Long tournamentId) {
        BasicFinancialSummaryResponse response = basicFinanceService.getSummary(tournamentId);
        return ResponseEntity.ok(ApiResponse.success(
                "BASIC_FINANCIAL_SUMMARY_FOUND",
                "Resumen financiero basico obtenido correctamente",
                response
        ));
    }
}
