package com.multideporte.backend.finance.dto.response;

import java.util.List;

public record FinancialMovementListResponse(
        Long tournamentId,
        int totalMovements,
        List<FinancialMovementResponse> movements
) {
}
