package com.multideporte.backend.stage.dto.request;

import com.multideporte.backend.stage.entity.TournamentStageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TournamentStageUpdateRequest(
        @NotBlank(message = "name es obligatorio")
        @Size(max = 100, message = "name no puede superar 100 caracteres")
        String name,

        @NotNull(message = "stageType es obligatorio")
        TournamentStageType stageType,

        @NotNull(message = "sequenceOrder es obligatorio")
        @Min(value = 1, message = "sequenceOrder debe ser mayor a 0")
        Integer sequenceOrder,

        @NotNull(message = "legs es obligatorio")
        @Min(value = 1, message = "legs debe ser mayor a 0")
        Integer legs,

        @NotNull(message = "roundTrip es obligatorio")
        Boolean roundTrip,

        @NotNull(message = "active es obligatorio")
        Boolean active
) {
}
