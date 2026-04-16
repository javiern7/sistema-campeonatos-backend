package com.multideporte.backend.reporting.service;

import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.reporting.dto.CardReportRow;
import com.multideporte.backend.reporting.dto.EventReportRow;
import com.multideporte.backend.reporting.dto.MatchReportRow;
import com.multideporte.backend.reporting.dto.OperationalReportResponse;
import com.multideporte.backend.reporting.dto.ReportExportResponse;
import com.multideporte.backend.reporting.dto.ScorerReportRow;
import com.multideporte.backend.reporting.dto.StandingReportRow;
import com.multideporte.backend.reporting.dto.TournamentSummaryReportRow;
import java.time.OffsetDateTime;

public interface OperationalReportingService {

    OperationalReportResponse<TournamentSummaryReportRow> getTournamentSummary(Long tournamentId);

    OperationalReportResponse<MatchReportRow> getMatchesReport(
            Long tournamentId,
            Long tournamentTeamId,
            Long teamId,
            MatchGameStatus status,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    );

    OperationalReportResponse<StandingReportRow> getStandingsReport(Long tournamentId);

    OperationalReportResponse<EventReportRow> getEventsReport(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    );

    OperationalReportResponse<ScorerReportRow> getScorersReport(Long tournamentId, Long tournamentTeamId, Long teamId);

    OperationalReportResponse<CardReportRow> getCardsReport(Long tournamentId, Long tournamentTeamId, Long teamId);

    ReportExportResponse exportFile(
            Long tournamentId,
            String reportType,
            String format,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            MatchGameStatus status,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    );
}
