package com.multideporte.backend.finance.dto.response;

public record FinancialTraceabilityResponse(
        String movementSource,
        String tournamentSource,
        String teamSource,
        String accountingScope
) {
}
