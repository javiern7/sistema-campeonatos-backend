package com.multideporte.backend.publicportal.dto;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record PublicTournamentCalendarResponse(
        Long tournamentId,
        String tournamentSlug,
        Long stageId,
        Long groupId,
        MatchGameStatus status,
        OffsetDateTime from,
        OffsetDateTime to,
        int totalMatches,
        int scheduledMatches,
        int closedMatches,
        List<PublicMatchSummaryResponse> matches
) {
}
