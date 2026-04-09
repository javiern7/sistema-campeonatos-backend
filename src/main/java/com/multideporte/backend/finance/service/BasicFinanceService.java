package com.multideporte.backend.finance.service;

import com.multideporte.backend.finance.dto.request.FinancialMovementCreateRequest;
import com.multideporte.backend.finance.dto.response.BasicFinancialSummaryResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementListResponse;
import com.multideporte.backend.finance.dto.response.FinancialMovementResponse;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;

public interface BasicFinanceService {

    FinancialMovementResponse createMovement(Long tournamentId, FinancialMovementCreateRequest request);

    FinancialMovementListResponse getMovements(
            Long tournamentId,
            FinancialMovementType movementType,
            FinancialMovementCategory category,
            Long tournamentTeamId
    );

    BasicFinancialSummaryResponse getSummary(Long tournamentId);
}
