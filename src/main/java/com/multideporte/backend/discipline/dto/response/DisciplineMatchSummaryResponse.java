package com.multideporte.backend.discipline.dto.response;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;

public record DisciplineMatchSummaryResponse(
        Long matchId,
        Long tournamentId,
        Long stageId,
        Long groupId,
        OffsetDateTime scheduledAt,
        MatchGameStatus status,
        DisciplineTeamResponse homeTeam,
        DisciplineTeamResponse awayTeam
) {
}
