package com.multideporte.backend.roster.dto.request;

import com.multideporte.backend.roster.entity.RosterStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TeamPlayerRosterCreateRequest(
        @NotNull(message = "tournamentTeamId es obligatorio")
        Long tournamentTeamId,

        @NotNull(message = "playerId es obligatorio")
        Long playerId,

        @Min(value = 0, message = "jerseyNumber no puede ser menor que 0")
        @Max(value = 99, message = "jerseyNumber no puede ser mayor que 99")
        Integer jerseyNumber,

        @NotNull(message = "captain es obligatorio")
        Boolean captain,

        @Size(max = 50, message = "positionName no puede superar 50 caracteres")
        String positionName,

        @NotNull(message = "rosterStatus es obligatorio")
        RosterStatus rosterStatus,

        @NotNull(message = "startDate es obligatorio")
        LocalDate startDate,

        LocalDate endDate
) {
}
