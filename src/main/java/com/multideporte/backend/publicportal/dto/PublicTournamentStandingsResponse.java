package com.multideporte.backend.publicportal.dto;

import java.util.List;

public record PublicTournamentStandingsResponse(
        Long tournamentId,
        String tournamentSlug,
        Long stageId,
        String stageName,
        String stageType,
        Long groupId,
        String groupCode,
        String groupName,
        int totalEntries,
        List<PublicStandingEntryResponse> standings
) {
}
