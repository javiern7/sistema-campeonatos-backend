package com.multideporte.backend.publicportal.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PublicPortalHomeResponse(
        String portalName,
        OffsetDateTime generatedAt,
        long visibleTournamentCount,
        long liveTournamentCount,
        long upcomingTournamentCount,
        long completedTournamentCount,
        List<PublicTournamentSummaryResponse> featuredTournaments,
        PublicReadModulesResponse modules
) {
}
