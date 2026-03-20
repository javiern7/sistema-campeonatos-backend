package com.multideporte.backend.stagegroup.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StageGroupUpdateRequest(
        @NotBlank(message = "code es obligatorio")
        @Size(max = 20, message = "code no puede superar 20 caracteres")
        String code,

        @NotBlank(message = "name es obligatorio")
        @Size(max = 50, message = "name no puede superar 50 caracteres")
        String name,

        @NotNull(message = "sequenceOrder es obligatorio")
        @Min(value = 1, message = "sequenceOrder debe ser mayor a 0")
        Integer sequenceOrder
) {
}
