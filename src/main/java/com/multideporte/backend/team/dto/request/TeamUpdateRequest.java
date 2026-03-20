package com.multideporte.backend.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamUpdateRequest(
        @NotBlank(message = "name es obligatorio")
        @Size(max = 150, message = "name no puede superar 150 caracteres")
        String name,

        @Size(max = 50, message = "shortName no puede superar 50 caracteres")
        String shortName,

        @Size(max = 30, message = "code no puede superar 30 caracteres")
        String code,

        @Size(max = 20, message = "primaryColor no puede superar 20 caracteres")
        String primaryColor,

        @Size(max = 20, message = "secondaryColor no puede superar 20 caracteres")
        String secondaryColor,

        @NotNull(message = "active es obligatorio")
        Boolean active
) {
}
