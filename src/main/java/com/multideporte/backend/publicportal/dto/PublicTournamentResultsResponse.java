package com.multideporte.backend.publicportal.dto;

import java.util.List;

public record PublicTournamentResultsResponse(
        Long tournamentId,
        String tournamentSlug,
        Long stageId,
        Long groupId,
        int totalClosedMatches,
        List<PublicResultEntryResponse> results
) {

    public record PublicResultEntryResponse(
            PublicMatchSummaryResponse match,
            boolean affectsStandings,
            String standingScope,
            String standingStatus
    ) {
    }
}
