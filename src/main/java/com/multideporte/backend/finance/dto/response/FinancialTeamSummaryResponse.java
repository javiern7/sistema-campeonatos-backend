package com.multideporte.backend.finance.dto.response;

import java.math.BigDecimal;

public record FinancialTeamSummaryResponse(
        FinancialTeamResponse team,
        BigDecimal incomeTotal,
        long movementCount
) {
}
