package com.multideporte.backend.tournament.dto.response;

import java.util.List;

public record TournamentKnockoutProgressionResponse(
        Long tournamentId,
        Long sourceStageId,
        Long targetStageId,
        Integer qualifiedTeamsCount,
        List<Long> qualifiedTournamentTeamIds
) {
}
