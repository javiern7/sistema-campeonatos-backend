package com.multideporte.backend.match.dto.request;

import com.multideporte.backend.match.entity.MatchGameStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MatchGameCreateRequest(
        @NotNull(message = "tournamentId es obligatorio")
        Long tournamentId,

        Long stageId,
        Long groupId,

        @Min(value = 1, message = "roundNumber debe ser mayor a 0")
        Integer roundNumber,

        @Min(value = 1, message = "matchdayNumber debe ser mayor a 0")
        Integer matchdayNumber,

        @NotNull(message = "homeTournamentTeamId es obligatorio")
        Long homeTournamentTeamId,

        @NotNull(message = "awayTournamentTeamId es obligatorio")
        Long awayTournamentTeamId,

        OffsetDateTime scheduledAt,

        @Size(max = 150, message = "venueName no puede superar 150 caracteres")
        String venueName,

        @NotNull(message = "status es obligatorio")
        MatchGameStatus status,

        Integer homeScore,
        Integer awayScore,
        Long winnerTournamentTeamId,
        String notes
) {
}
