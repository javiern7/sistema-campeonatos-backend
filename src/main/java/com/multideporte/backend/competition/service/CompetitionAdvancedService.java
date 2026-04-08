package com.multideporte.backend.competition.service;

import com.multideporte.backend.competition.dto.response.CompetitionAdvancedBracketResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedCalendarResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedResultsResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;

public interface CompetitionAdvancedService {

    CompetitionAdvancedBracketResponse getBracket(Long tournamentId, Long stageId);

    CompetitionAdvancedCalendarResponse getCalendar(
            Long tournamentId,
            Long stageId,
            Long groupId,
            MatchGameStatus status,
            OffsetDateTime from,
            OffsetDateTime to
    );

    CompetitionAdvancedResultsResponse getResults(Long tournamentId, Long stageId, Long groupId);
}
