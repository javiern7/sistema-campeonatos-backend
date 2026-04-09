package com.multideporte.backend.finance.dto.request;

import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialMovementCreateRequest(
        Long tournamentTeamId,

        @NotNull(message = "movementType es obligatorio")
        FinancialMovementType movementType,

        @NotNull(message = "category es obligatorio")
        FinancialMovementCategory category,

        @NotNull(message = "amount es obligatorio")
        @DecimalMin(value = "0.01", message = "amount debe ser mayor a cero")
        BigDecimal amount,

        @NotNull(message = "occurredOn es obligatorio")
        LocalDate occurredOn,

        @Size(max = 300, message = "description no puede superar 300 caracteres")
        String description,

        @Size(max = 80, message = "referenceCode no puede superar 80 caracteres")
        String referenceCode
) {
}
