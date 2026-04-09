package com.multideporte.backend.finance.dto.response;

public record FinancialTeamResponse(
        Long tournamentTeamId,
        Long teamId,
        String name,
        String shortName,
        String code
) {
}
