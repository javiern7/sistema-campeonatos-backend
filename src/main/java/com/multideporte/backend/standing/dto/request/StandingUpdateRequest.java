package com.multideporte.backend.standing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StandingUpdateRequest(
        Long stageId,
        Long groupId,

        @NotNull(message = "tournamentTeamId es obligatorio")
        Long tournamentTeamId,

        @NotNull(message = "played es obligatorio")
        @Min(value = 0, message = "played no puede ser negativo")
        Integer played,

        @NotNull(message = "wins es obligatorio")
        @Min(value = 0, message = "wins no puede ser negativo")
        Integer wins,

        @NotNull(message = "draws es obligatorio")
        @Min(value = 0, message = "draws no puede ser negativo")
        Integer draws,

        @NotNull(message = "losses es obligatorio")
        @Min(value = 0, message = "losses no puede ser negativo")
        Integer losses,

        @NotNull(message = "pointsFor es obligatorio")
        @Min(value = 0, message = "pointsFor no puede ser negativo")
        Integer pointsFor,

        @NotNull(message = "pointsAgainst es obligatorio")
        @Min(value = 0, message = "pointsAgainst no puede ser negativo")
        Integer pointsAgainst,

        @NotNull(message = "scoreDiff es obligatorio")
        Integer scoreDiff,

        @NotNull(message = "points es obligatorio")
        @Min(value = 0, message = "points no puede ser negativo")
        Integer points,

        @Min(value = 1, message = "rankPosition debe ser mayor a 0")
        Integer rankPosition
) {
}
