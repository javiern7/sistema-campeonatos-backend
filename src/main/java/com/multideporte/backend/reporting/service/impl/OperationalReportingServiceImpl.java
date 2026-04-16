package com.multideporte.backend.reporting.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import com.multideporte.backend.matchevent.entity.MatchEventType;
import com.multideporte.backend.matchevent.repository.MatchEventRepository;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.reporting.dto.CardReportRow;
import com.multideporte.backend.reporting.dto.EventReportRow;
import com.multideporte.backend.reporting.dto.MatchReportRow;
import com.multideporte.backend.reporting.dto.OperationalReportResponse;
import com.multideporte.backend.reporting.dto.ReportExportResponse;
import com.multideporte.backend.reporting.dto.ReportFiltersResponse;
import com.multideporte.backend.reporting.dto.ReportMetadataResponse;
import com.multideporte.backend.reporting.dto.ReportTournamentResponse;
import com.multideporte.backend.reporting.dto.ScorerReportRow;
import com.multideporte.backend.reporting.dto.StandingReportRow;
import com.multideporte.backend.reporting.dto.TournamentSummaryReportRow;
import com.multideporte.backend.reporting.service.OperationalReportingService;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationalReportingServiceImpl implements OperationalReportingService {

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final List<String> REPORT_RULES = List.of(
            "Read-only desde datos consolidados",
            "No recalcula standings",
            "No modifica resultados oficiales",
            "Eventos ANNULLED excluidos de rankings y totales derivados"
    );

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;
    private final MatchEventRepository matchEventRepository;

    @Override
    public OperationalReportResponse<TournamentSummaryReportRow> getTournamentSummary(Long tournamentId) {
        Tournament tournament = getTournament(tournamentId);
        List<TournamentTeam> tournamentTeams = tournamentTeamRepository.findAllByTournamentId(tournamentId);
        List<MatchGame> matches = matchGameRepository.findAllByTournamentIdOrderByScheduledAtAscIdAsc(tournamentId);
        List<Standing> standings = standingRepository.findAllByTournamentIdOrderByRankPositionAsc(tournamentId);
        EventCounts events = countEvents(loadActiveEvents(tournamentId, null, null, null, null, null, null));
        TournamentSummaryReportRow row = new TournamentSummaryReportRow(
                tournament.getId(),
                tournament.getName(),
                (long) tournamentTeams.size(),
                (long) matches.size(),
                (long) standings.size(),
                events.activeEvents(),
                events.goals(),
                events.yellowCards(),
                events.redCards()
        );
        return report("tournament-summary", tournament, filters(tournamentId, null, null, null, null, null, null),
                Map.of("rows", 1, "teams", tournamentTeams.size(), "matches", matches.size()), List.of(row));
    }

    @Override
    public OperationalReportResponse<MatchReportRow> getMatchesReport(
            Long tournamentId,
            Long tournamentTeamId,
            Long teamId,
            MatchGameStatus status,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    ) {
        Tournament tournament = getTournament(tournamentId);
        validateTeamFilter(tournamentId, tournamentTeamId, teamId);
        Long resolvedTournamentTeamId = resolveTournamentTeamId(tournamentId, tournamentTeamId, teamId);
        List<MatchGame> matches = matchGameRepository.findAllByTournamentIdOrderByScheduledAtAscIdAsc(tournamentId)
                .stream()
                .filter(match -> status == null || match.getStatus() == status)
                .filter(match -> resolvedTournamentTeamId == null
                        || Objects.equals(match.getHomeTournamentTeamId(), resolvedTournamentTeamId)
                        || Objects.equals(match.getAwayTournamentTeamId(), resolvedTournamentTeamId))
                .filter(match -> scheduledFrom == null
                        || (match.getScheduledAt() != null && !match.getScheduledAt().isBefore(scheduledFrom)))
                .filter(match -> scheduledTo == null
                        || (match.getScheduledAt() != null && !match.getScheduledAt().isAfter(scheduledTo)))
                .toList();
        Map<Long, TeamName> teamNames = teamNamesByTournamentTeamId(matches.stream()
                .flatMap(match -> List.of(match.getHomeTournamentTeamId(), match.getAwayTournamentTeamId()).stream())
                .collect(Collectors.toSet()));
        List<MatchReportRow> rows = matches.stream()
                .map(match -> toMatchRow(match, teamNames))
                .toList();
        return report("matches", tournament, filters(tournamentId, null, tournamentTeamId, teamId, null, scheduledFrom, scheduledTo),
                Map.of("rows", rows.size()), rows);
    }

    @Override
    public OperationalReportResponse<StandingReportRow> getStandingsReport(Long tournamentId) {
        Tournament tournament = getTournament(tournamentId);
        List<Standing> standings = standingRepository.findAllByTournamentIdOrderByRankPositionAsc(tournamentId);
        Map<Long, TeamName> teamNames = teamNamesByTournamentTeamId(standings.stream()
                .map(Standing::getTournamentTeamId)
                .collect(Collectors.toSet()));
        List<StandingReportRow> rows = standings.stream()
                .map(standing -> toStandingRow(standing, teamNames.get(standing.getTournamentTeamId())))
                .toList();
        return report("standings", tournament, filters(tournamentId, null, null, null, null, null, null),
                Map.of("rows", rows.size()), rows);
    }

    @Override
    public OperationalReportResponse<EventReportRow> getEventsReport(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    ) {
        Tournament tournament = getTournament(tournamentId);
        validateFilters(tournamentId, matchId, tournamentTeamId, teamId, playerId);
        List<MatchEvent> events = loadActiveEvents(tournamentId, matchId, tournamentTeamId, teamId, playerId, scheduledFrom, scheduledTo);
        Enrichment enrichment = enrich(events);
        List<EventReportRow> rows = events.stream()
                .map(event -> toEventRow(event, enrichment))
                .toList();
        EventCounts counts = countEvents(events);
        return report("events", tournament, filters(tournamentId, matchId, tournamentTeamId, teamId, playerId, scheduledFrom, scheduledTo),
                Map.of("rows", rows.size(), "goals", counts.goals(), "yellowCards", counts.yellowCards(), "redCards", counts.redCards()),
                rows);
    }

    @Override
    public OperationalReportResponse<ScorerReportRow> getScorersReport(Long tournamentId, Long tournamentTeamId, Long teamId) {
        Tournament tournament = getTournament(tournamentId);
        validateTeamFilter(tournamentId, tournamentTeamId, teamId);
        List<MatchEvent> events = loadActiveEvents(tournamentId, null, tournamentTeamId, teamId, null, null, null).stream()
                .filter(event -> event.getEventType() == MatchEventType.SCORE && event.getPlayerId() != null)
                .toList();
        Enrichment enrichment = enrich(events);
        List<ScorerReportRow> rows = events.stream()
                .collect(Collectors.groupingBy(event -> new PlayerTeamKey(event.getPlayerId(), event.getTournamentTeamId()),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toScorerRow(entry.getKey(), entry.getValue(), enrichment))
                .sorted(Comparator.comparing(ScorerReportRow::goals).reversed()
                        .thenComparing(ScorerReportRow::playerName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return report("scorers", tournament, filters(tournamentId, null, tournamentTeamId, teamId, null, null, null),
                Map.of("rows", rows.size(), "goals", rows.stream().mapToInt(ScorerReportRow::goals).sum()), rows);
    }

    @Override
    public OperationalReportResponse<CardReportRow> getCardsReport(Long tournamentId, Long tournamentTeamId, Long teamId) {
        Tournament tournament = getTournament(tournamentId);
        validateTeamFilter(tournamentId, tournamentTeamId, teamId);
        List<MatchEvent> events = loadActiveEvents(tournamentId, null, tournamentTeamId, teamId, null, null, null).stream()
                .filter(event -> event.getPlayerId() != null
                        && (event.getEventType() == MatchEventType.YELLOW_CARD || event.getEventType() == MatchEventType.RED_CARD))
                .toList();
        Enrichment enrichment = enrich(events);
        List<CardReportRow> rows = events.stream()
                .collect(Collectors.groupingBy(event -> new PlayerTeamKey(event.getPlayerId(), event.getTournamentTeamId()),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toCardRow(entry.getKey(), entry.getValue(), enrichment))
                .sorted(Comparator.comparing(CardReportRow::totalCards).reversed()
                        .thenComparing(CardReportRow::playerName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return report("cards", tournament, filters(tournamentId, null, tournamentTeamId, teamId, null, null, null),
                Map.of("rows", rows.size(), "cards", rows.stream().mapToInt(CardReportRow::totalCards).sum()), rows);
    }

    @Override
    public ReportExportResponse exportFile(
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
    ) {
        String normalizedReportType = normalizeReportType(reportType);
        String normalizedFormat = normalizeFormat(format);
        TableData table = buildTable(
                tournamentId,
                normalizedReportType,
                matchId,
                tournamentTeamId,
                teamId,
                playerId,
                status,
                scheduledFrom,
                scheduledTo
        );

        byte[] content = switch (normalizedFormat) {
            case "csv" -> tableToCsv(table).getBytes(StandardCharsets.UTF_8);
            case "xlsx" -> tableToExcel(table);
            case "pdf" -> tableToPdf(table);
            default -> throw new BusinessException("Formato no soportado. Formatos permitidos: csv, xlsx, pdf");
        };
        String contentType = switch (normalizedFormat) {
            case "csv" -> CSV_CONTENT_TYPE;
            case "xlsx" -> XLSX_CONTENT_TYPE;
            case "pdf" -> PDF_CONTENT_TYPE;
            default -> throw new BusinessException("Formato no soportado. Formatos permitidos: csv, xlsx, pdf");
        };
        return new ReportExportResponse(
                "tournament-" + tournamentId + "-" + normalizedReportType + "." + normalizedFormat,
                contentType,
                content
        );
    }

    private Tournament getTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado con id: " + tournamentId));
    }

    private void validateFilters(Long tournamentId, Long matchId, Long tournamentTeamId, Long teamId, Long playerId) {
        if (matchId != null) {
            MatchGame match = matchGameRepository.findById(matchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Partido no encontrado con id: " + matchId));
            if (!Objects.equals(match.getTournamentId(), tournamentId)) {
                throw new BusinessException("El partido indicado no pertenece al torneo solicitado");
            }
        }
        validateTeamFilter(tournamentId, tournamentTeamId, teamId);
        if (playerId != null && !playerRepository.existsById(playerId)) {
            throw new ResourceNotFoundException("Jugador no encontrado con id: " + playerId);
        }
    }

    private void validateTeamFilter(Long tournamentId, Long tournamentTeamId, Long teamId) {
        if (tournamentTeamId != null) {
            TournamentTeam tournamentTeam = tournamentTeamRepository.findById(tournamentTeamId)
                    .orElseThrow(() -> new ResourceNotFoundException("TournamentTeam no encontrado con id: " + tournamentTeamId));
            if (!Objects.equals(tournamentTeam.getTournamentId(), tournamentId)) {
                throw new BusinessException("El tournamentTeamId indicado no pertenece al torneo solicitado");
            }
            if (teamId != null && !Objects.equals(tournamentTeam.getTeamId(), teamId)) {
                throw new BusinessException("teamId y tournamentTeamId no corresponden al mismo equipo inscrito");
            }
        } else if (teamId != null) {
            if (!teamRepository.existsById(teamId)) {
                throw new ResourceNotFoundException("Equipo no encontrado con id: " + teamId);
            }
            tournamentTeamRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                    .orElseThrow(() -> new BusinessException("El equipo indicado no esta inscrito en el torneo solicitado"));
        }
    }

    private Long resolveTournamentTeamId(Long tournamentId, Long tournamentTeamId, Long teamId) {
        if (tournamentTeamId != null) {
            return tournamentTeamId;
        }
        if (teamId == null) {
            return null;
        }
        return tournamentTeamRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .map(TournamentTeam::getId)
                .orElse(null);
    }

    private List<MatchEvent> loadActiveEvents(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    ) {
        return matchEventRepository.findReportEvents(
                tournamentId, MatchEventStatus.ACTIVE, matchId, tournamentTeamId, teamId, playerId)
                .stream()
                .filter(event -> matchesScheduledRange(event, scheduledFrom, scheduledTo))
                .toList();
    }

    private boolean matchesScheduledRange(MatchEvent event, OffsetDateTime scheduledFrom, OffsetDateTime scheduledTo) {
        if (scheduledFrom == null && scheduledTo == null) {
            return true;
        }
        MatchGame match = matchGameRepository.findById(event.getMatchId()).orElse(null);
        if (match == null || match.getScheduledAt() == null) {
            return false;
        }
        return (scheduledFrom == null || !match.getScheduledAt().isBefore(scheduledFrom))
                && (scheduledTo == null || !match.getScheduledAt().isAfter(scheduledTo));
    }

    private <T> OperationalReportResponse<T> report(
            String reportType,
            Tournament tournament,
            ReportFiltersResponse filters,
            Map<String, Object> totals,
            List<T> rows
    ) {
        return new OperationalReportResponse<>(
                new ReportMetadataResponse(reportType, "json", Instant.now(), sourceFor(reportType), REPORT_RULES),
                toTournament(tournament),
                filters,
                totals,
                rows
        );
    }

    private String sourceFor(String reportType) {
        if ("standings".equals(reportType)) {
            return "standing";
        }
        if ("events".equals(reportType) || "scorers".equals(reportType) || "cards".equals(reportType)) {
            return "match_event";
        }
        return "tournament, tournament_team, match_game, standing, match_event";
    }

    private ReportTournamentResponse toTournament(Tournament tournament) {
        return new ReportTournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getSeasonName(),
                tournament.getFormat().name(),
                tournament.getStatus().name(),
                tournament.getOperationalCategory().name()
        );
    }

    private ReportFiltersResponse filters(
            Long tournamentId,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    ) {
        return new ReportFiltersResponse(tournamentId, matchId, tournamentTeamId, teamId, playerId, scheduledFrom, scheduledTo);
    }

    private MatchReportRow toMatchRow(MatchGame match, Map<Long, TeamName> teamNames) {
        return new MatchReportRow(
                match.getId(),
                match.getStageId(),
                match.getGroupId(),
                match.getRoundNumber(),
                match.getMatchdayNumber(),
                match.getHomeTournamentTeamId(),
                teamName(teamNames.get(match.getHomeTournamentTeamId())),
                match.getAwayTournamentTeamId(),
                teamName(teamNames.get(match.getAwayTournamentTeamId())),
                match.getScheduledAt(),
                match.getVenueName(),
                match.getStatus().name(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getWinnerTournamentTeamId()
        );
    }

    private StandingReportRow toStandingRow(Standing standing, TeamName teamName) {
        return new StandingReportRow(
                standing.getId(),
                standing.getRankPosition(),
                standing.getStageId(),
                standing.getGroupId(),
                standing.getTournamentTeamId(),
                teamName != null ? teamName.teamId() : null,
                teamName(teamName),
                standing.getPlayed(),
                standing.getWins(),
                standing.getDraws(),
                standing.getLosses(),
                standing.getPointsFor(),
                standing.getPointsAgainst(),
                standing.getScoreDiff(),
                standing.getPoints()
        );
    }

    private EventReportRow toEventRow(MatchEvent event, Enrichment enrichment) {
        TournamentTeam tournamentTeam = enrichment.tournamentTeamsById().get(event.getTournamentTeamId());
        Team team = tournamentTeam != null ? enrichment.teamsById().get(tournamentTeam.getTeamId()) : null;
        Player player = enrichment.playersById().get(event.getPlayerId());
        return new EventReportRow(
                event.getId(),
                event.getMatchId(),
                event.getTournamentTeamId(),
                tournamentTeam != null ? tournamentTeam.getTeamId() : null,
                team != null ? team.getName() : null,
                event.getPlayerId(),
                playerName(player),
                event.getEventType().name(),
                event.getStatus().name(),
                event.getPeriodLabel(),
                event.getEventMinute(),
                event.getEventSecond(),
                event.getEventValue(),
                event.getCreatedAt()
        );
    }

    private ScorerReportRow toScorerRow(PlayerTeamKey key, List<MatchEvent> events, Enrichment enrichment) {
        TournamentTeam tournamentTeam = enrichment.tournamentTeamsById().get(key.tournamentTeamId());
        Team team = tournamentTeam != null ? enrichment.teamsById().get(tournamentTeam.getTeamId()) : null;
        Player player = enrichment.playersById().get(key.playerId());
        int goals = events.stream().mapToInt(event -> event.getEventValue() != null ? event.getEventValue() : 1).sum();
        return new ScorerReportRow(key.playerId(), playerName(player), key.tournamentTeamId(),
                tournamentTeam != null ? tournamentTeam.getTeamId() : null, team != null ? team.getName() : null, goals);
    }

    private CardReportRow toCardRow(PlayerTeamKey key, List<MatchEvent> events, Enrichment enrichment) {
        TournamentTeam tournamentTeam = enrichment.tournamentTeamsById().get(key.tournamentTeamId());
        Team team = tournamentTeam != null ? enrichment.teamsById().get(tournamentTeam.getTeamId()) : null;
        Player player = enrichment.playersById().get(key.playerId());
        int yellow = (int) events.stream().filter(event -> event.getEventType() == MatchEventType.YELLOW_CARD).count();
        int red = (int) events.stream().filter(event -> event.getEventType() == MatchEventType.RED_CARD).count();
        return new CardReportRow(key.playerId(), playerName(player), key.tournamentTeamId(),
                tournamentTeam != null ? tournamentTeam.getTeamId() : null, team != null ? team.getName() : null,
                yellow, red, yellow + red);
    }

    private Enrichment enrich(List<MatchEvent> events) {
        Set<Long> tournamentTeamIds = events.stream().map(MatchEvent::getTournamentTeamId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> playerIds = events.stream().map(MatchEvent::getPlayerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, TournamentTeam> tournamentTeamsById = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Set<Long> teamIds = tournamentTeamsById.values().stream().map(TournamentTeam::getTeamId).collect(Collectors.toSet());
        Map<Long, Team> teamsById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, Player> playersById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        return new Enrichment(tournamentTeamsById, teamsById, playersById);
    }

    private Map<Long, TeamName> teamNamesByTournamentTeamId(Set<Long> tournamentTeamIds) {
        Map<Long, TournamentTeam> tournamentTeamsById = tournamentTeamRepository.findAllById(tournamentTeamIds).stream()
                .collect(Collectors.toMap(TournamentTeam::getId, Function.identity()));
        Map<Long, Team> teamsById = teamRepository.findAllById(tournamentTeamsById.values().stream()
                        .map(TournamentTeam::getTeamId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));
        Map<Long, TeamName> names = new LinkedHashMap<>();
        tournamentTeamsById.forEach((id, tournamentTeam) -> {
            Team team = teamsById.get(tournamentTeam.getTeamId());
            names.put(id, new TeamName(tournamentTeam.getTeamId(), team != null ? team.getName() : null));
        });
        return names;
    }

    private EventCounts countEvents(List<MatchEvent> events) {
        int goals = 0;
        int yellow = 0;
        int red = 0;
        for (MatchEvent event : events) {
            if (event.getEventType() == MatchEventType.SCORE) {
                goals += event.getEventValue() != null ? event.getEventValue() : 1;
            } else if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                yellow++;
            } else if (event.getEventType() == MatchEventType.RED_CARD) {
                red++;
            }
        }
        return new EventCounts(events.size(), goals, yellow, red);
    }

    private String normalizeReportType(String reportType) {
        String normalized = reportType == null ? "" : reportType.trim().toLowerCase();
        if ("summary".equals(normalized)) {
            return "tournament-summary";
        }
        return normalized;
    }

    private String normalizeFormat(String format) {
        return format == null || format.isBlank() ? "csv" : format.trim().toLowerCase();
    }

    private TableData buildTable(
            Long tournamentId,
            String reportType,
            Long matchId,
            Long tournamentTeamId,
            Long teamId,
            Long playerId,
            MatchGameStatus status,
            OffsetDateTime scheduledFrom,
            OffsetDateTime scheduledTo
    ) {
        return switch (reportType) {
            case "tournament-summary" -> summaryTable(getTournamentSummary(tournamentId).rows());
            case "matches" -> matchesTable(getMatchesReport(tournamentId, tournamentTeamId, teamId, status, scheduledFrom, scheduledTo).rows());
            case "standings" -> standingsTable(getStandingsReport(tournamentId).rows());
            case "events" -> eventsTable(getEventsReport(tournamentId, matchId, tournamentTeamId, teamId, playerId, scheduledFrom, scheduledTo).rows());
            case "scorers" -> scorersTable(getScorersReport(tournamentId, tournamentTeamId, teamId).rows());
            case "cards" -> cardsTable(getCardsReport(tournamentId, tournamentTeamId, teamId).rows());
            default -> throw new BusinessException("Tipo de reporte no soportado para exportacion: " + reportType);
        };
    }

    private TableData summaryTable(List<TournamentSummaryReportRow> rows) {
        return new TableData(
                "Resumen",
                List.of("tournamentId", "tournamentName", "teams", "matches", "standings", "activeEvents", "goals", "yellowCards", "redCards"),
                rows.stream().map(row -> List.<Object>of(row.tournamentId(), row.tournamentName(), row.teams(), row.matches(),
                        row.standings(), row.activeEvents(), row.goals(), row.yellowCards(), row.redCards())).toList()
        );
    }

    private TableData matchesTable(List<MatchReportRow> rows) {
        return new TableData(
                "Partidos",
                List.of("matchId", "homeTeamName", "awayTeamName", "scheduledAt", "status", "homeScore", "awayScore", "venueName"),
                rows.stream().map(row -> List.<Object>of(row.matchId(), nullable(row.homeTeamName()), nullable(row.awayTeamName()),
                        nullable(row.scheduledAt()), row.status(), nullable(row.homeScore()), nullable(row.awayScore()),
                        nullable(row.venueName()))).toList()
        );
    }

    private TableData standingsTable(List<StandingReportRow> rows) {
        return new TableData(
                "Standings",
                List.of("rankPosition", "teamName", "played", "wins", "draws", "losses", "pointsFor", "pointsAgainst", "scoreDiff", "points"),
                rows.stream().map(row -> List.<Object>of(nullable(row.rankPosition()), nullable(row.teamName()), row.played(), row.wins(), row.draws(),
                        row.losses(), row.pointsFor(), row.pointsAgainst(), row.scoreDiff(), row.points())).toList()
        );
    }

    private TableData eventsTable(List<EventReportRow> rows) {
        return new TableData(
                "Eventos",
                List.of("eventId", "matchId", "teamName", "playerName", "eventType", "periodLabel", "eventMinute", "eventSecond", "eventValue", "createdAt"),
                rows.stream().map(row -> List.<Object>of(row.eventId(), row.matchId(), nullable(row.teamName()), nullable(row.playerName()), row.eventType(),
                        nullable(row.periodLabel()), nullable(row.eventMinute()), nullable(row.eventSecond()), nullable(row.eventValue()),
                        nullable(row.createdAt()))).toList()
        );
    }

    private TableData scorersTable(List<ScorerReportRow> rows) {
        return new TableData(
                "Goleadores",
                List.of("playerName", "teamName", "goals"),
                rows.stream().map(row -> List.<Object>of(nullable(row.playerName()), nullable(row.teamName()), row.goals())).toList()
        );
    }

    private TableData cardsTable(List<CardReportRow> rows) {
        return new TableData(
                "Tarjetas",
                List.of("playerName", "teamName", "yellowCards", "redCards", "totalCards"),
                rows.stream().map(row -> List.<Object>of(nullable(row.playerName()), nullable(row.teamName()), row.yellowCards(), row.redCards(), row.totalCards())).toList()
        );
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private String tableToCsv(TableData table) {
        List<String> rows = table.rows().stream()
                .map(row -> csvRow(row.toArray()))
                .toList();
        return csvRow(table.headers().toArray()) + "\r\n" + String.join("\r\n", rows) + (rows.isEmpty() ? "" : "\r\n");
    }

    private String csvRow(Object... values) {
        return Arrays.stream(values)
                .map(this::csvCell)
                .collect(Collectors.joining(","));
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private byte[] tableToExcel(TableData table) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(table.sheetName());
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<Object> values = table.rows().get(rowIndex);
                for (int cellIndex = 0; cellIndex < values.size(); cellIndex++) {
                    row.createCell(cellIndex).setCellValue(String.valueOf(values.get(cellIndex)));
                }
            }

            for (int i = 0; i < table.headers().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("No se pudo generar el archivo Excel");
        }
    }

    private byte[] tableToPdf(TableData table) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph(table.sheetName(), new Font(Font.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph("Generado: " + Instant.now(), new Font(Font.HELVETICA, 9)));
            document.add(new Paragraph(" "));

            if (table.rows().isEmpty()) {
                document.add(new Paragraph("Sin filas para los filtros indicados."));
            } else {
                PdfPTable pdfTable = new PdfPTable(table.headers().size());
                pdfTable.setWidthPercentage(100);
                for (String header : table.headers()) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.HELVETICA, 8, Font.BOLD)));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    pdfTable.addCell(cell);
                }
                for (List<Object> row : table.rows()) {
                    for (Object value : row) {
                        pdfTable.addCell(new Phrase(String.valueOf(value), new Font(Font.HELVETICA, 8)));
                    }
                }
                document.add(pdfTable);
            }
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new BusinessException("No se pudo generar el archivo PDF");
        }
    }

    private String teamName(TeamName teamName) {
        return teamName != null ? teamName.name() : null;
    }

    private String playerName(Player player) {
        return player != null ? (player.getFirstName() + " " + player.getLastName()).trim() : null;
    }

    private record TeamName(Long teamId, String name) {
    }

    private record PlayerTeamKey(Long playerId, Long tournamentTeamId) {
    }

    private record EventCounts(Integer activeEvents, Integer goals, Integer yellowCards, Integer redCards) {
    }

    private record Enrichment(
            Map<Long, TournamentTeam> tournamentTeamsById,
            Map<Long, Team> teamsById,
            Map<Long, Player> playersById
    ) {
    }

    private record TableData(
            String sheetName,
            List<String> headers,
            List<List<Object>> rows
    ) {
    }
}
