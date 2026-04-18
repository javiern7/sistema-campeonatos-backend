package com.multideporte.backend.security.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OperationalUserRolesUpdateRequest(
        @NotEmpty(message = "Se requiere al menos un rol para el usuario")
        List<@NotBlank(message = "Los codigos de rol no pueden estar vacios") String> roleCodes,

        @NotBlank(message = "La razon operativa del cambio es obligatoria")
        String reason
) {
}
