package com.multideporte.backend.sport.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SportPositionCreateRequest(
        @NotBlank(message = "code es obligatorio")
        @Size(max = 30, message = "code no puede superar 30 caracteres")
        String code,

        @NotBlank(message = "name es obligatorio")
        @Size(max = 80, message = "name no puede superar 80 caracteres")
        String name,

        @NotNull(message = "displayOrder es obligatorio")
        @Min(value = 1, message = "displayOrder debe ser mayor a 0")
        Integer displayOrder,

        @NotNull(message = "active es obligatorio")
        Boolean active
) {
}
