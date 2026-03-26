package com.multideporte.backend.tournament.dto.response;

import com.multideporte.backend.tournament.dto.request.KnockoutSeedingStrategy;
import java.time.OffsetDateTime;
import java.util.List;

public record TournamentKnockoutBracketResponse(
        Long tournamentId,
        Long stageId,
        KnockoutSeedingStrategy seedingStrategy,
        Integer roundNumber,
        Integer matchdayNumber,
        int generatedMatchesCount,
        OffsetDateTime generatedAt,
        List<GeneratedKnockoutMatch> generatedMatches
) {

    public record GeneratedKnockoutMatch(
            Long matchId,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            Integer homeSeedPosition,
            Integer awaySeedPosition,
            Long homeSourceGroupId,
            Long awaySourceGroupId,
            Integer homeSourceGroupOrder,
            Integer awaySourceGroupOrder,
            String notes
    ) {
    }
}
