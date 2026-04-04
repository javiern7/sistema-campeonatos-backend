package com.multideporte.backend.tournament.dto.request;

import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TournamentCreateRequest(
        @NotNull(message = "sportId es obligatorio")
        Long sportId,

        @NotBlank(message = "name es obligatorio")
        @Size(max = 150, message = "name no puede superar 150 caracteres")
        String name,

        @NotBlank(message = "seasonName es obligatorio")
        @Size(max = 50, message = "seasonName no puede superar 50 caracteres")
        String seasonName,

        @NotNull(message = "format es obligatorio")
        TournamentFormat format,

        @NotNull(message = "status es obligatorio")
        TournamentStatus status,

        TournamentOperationalCategory operationalCategory,

        @Size(max = 4000, message = "description es demasiado larga")
        String description,

        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime registrationOpenAt,
        OffsetDateTime registrationCloseAt,

        @Min(value = 2, message = "maxTeams debe ser mayor o igual a 2")
        Integer maxTeams,

        @NotNull(message = "pointsWin es obligatorio")
        Integer pointsWin,

        @NotNull(message = "pointsDraw es obligatorio")
        Integer pointsDraw,

        @NotNull(message = "pointsLoss es obligatorio")
        Integer pointsLoss
) {
}
