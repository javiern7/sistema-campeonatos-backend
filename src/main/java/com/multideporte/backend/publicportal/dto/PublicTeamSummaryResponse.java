package com.multideporte.backend.publicportal.dto;

public record PublicTeamSummaryResponse(
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        String shortName,
        String code,
        Integer seedNumber
) {
}
