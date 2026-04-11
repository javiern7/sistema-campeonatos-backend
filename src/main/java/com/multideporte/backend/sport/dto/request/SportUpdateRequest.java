package com.multideporte.backend.sport.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SportUpdateRequest(
        @NotBlank(message = "code es obligatorio")
        @Size(max = 30, message = "code no puede superar 30 caracteres")
        String code,

        @NotBlank(message = "name es obligatorio")
        @Size(max = 100, message = "name no puede superar 100 caracteres")
        String name,

        @NotNull(message = "teamBased es obligatorio")
        Boolean teamBased,

        @Min(value = 1, message = "maxPlayersOnField debe ser mayor a 0")
        Integer maxPlayersOnField,

        @Size(max = 30, message = "scoreLabel no puede superar 30 caracteres")
        String scoreLabel,

        @NotNull(message = "active es obligatorio")
        Boolean active
) {
}
