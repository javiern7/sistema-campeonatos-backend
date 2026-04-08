package com.multideporte.backend.competition.dto.response;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record CompetitionAdvancedCalendarResponse(
        Long tournamentId,
        Long stageId,
        Long groupId,
        MatchGameStatus status,
        OffsetDateTime from,
        OffsetDateTime to,
        int totalMatches,
        int scheduledMatches,
        int closedMatches,
        List<CompetitionAdvancedMatchSummary> matches
) {
}
