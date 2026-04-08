package com.multideporte.backend.security.governance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RolePermissionUpdateRequest(
        @NotEmpty(message = "Se requiere al menos un permiso para el rol")
        List<@NotBlank(message = "Los codigos de permiso no pueden estar vacios") String> permissionCodes,
        @NotBlank(message = "La razon operativa del cambio es obligatoria")
        String reason
) {
}
