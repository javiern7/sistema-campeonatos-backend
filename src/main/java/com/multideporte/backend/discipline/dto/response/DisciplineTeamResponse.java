package com.multideporte.backend.discipline.dto.response;

public record DisciplineTeamResponse(
        Long tournamentTeamId,
        Long teamId,
        String name,
        String shortName,
        String code
) {
}
