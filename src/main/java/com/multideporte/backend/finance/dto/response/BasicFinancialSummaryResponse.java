package com.multideporte.backend.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BasicFinancialSummaryResponse(
        Long tournamentId,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        long movementCount,
        List<FinancialCategorySummaryResponse> byCategory,
        List<FinancialTeamSummaryResponse> incomeByTeam,
        FinancialTraceabilityResponse traceability
) {
}
