package com.multideporte.backend.finance.dto.response;

import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import java.math.BigDecimal;

public record FinancialCategorySummaryResponse(
        FinancialMovementType movementType,
        FinancialMovementCategory category,
        BigDecimal totalAmount,
        long movementCount
) {
}
