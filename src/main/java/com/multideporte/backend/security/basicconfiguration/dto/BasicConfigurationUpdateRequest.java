package com.multideporte.backend.security.basicconfiguration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BasicConfigurationUpdateRequest(
        @NotBlank(message = "organizationName es obligatorio")
        @Size(max = 120, message = "organizationName no puede superar 120 caracteres")
        String organizationName,

        @NotBlank(message = "supportEmail es obligatorio")
        @Email(message = "supportEmail debe ser un email valido")
        @Size(max = 150, message = "supportEmail no puede superar 150 caracteres")
        String supportEmail,

        @NotBlank(message = "defaultTimezone es obligatorio")
        @Size(max = 60, message = "defaultTimezone no puede superar 60 caracteres")
        String defaultTimezone
) {
}
