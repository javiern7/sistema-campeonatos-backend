package com.multideporte.backend.tournament.dto.response;

import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.util.List;

public record TournamentOperationalSummaryResponse(
        Long tournamentId,
        String tournamentName,
        TournamentStatus tournamentStatus,
        TournamentOperationalCategory operationalCategory,
        boolean executiveReportingEligible,
        boolean integrityHealthy,
        long approvedTeams,
        long approvedTeamsWithActiveRosterSupport,
        long approvedTeamsMissingActiveRosterSupport,
        long closedMatches,
        long generatedStandings,
        List<String> integrityAlerts
) {
}
