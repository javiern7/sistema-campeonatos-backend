package com.multideporte.backend.finance.dto.response;

import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FinancialMovementResponse(
        Long movementId,
        Long tournamentId,
        FinancialTeamResponse team,
        FinancialMovementType movementType,
        FinancialMovementCategory category,
        BigDecimal amount,
        LocalDate occurredOn,
        String description,
        String referenceCode,
        OffsetDateTime createdAt
) {
}
