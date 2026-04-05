package com.multideporte.backend.security.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank(message = "username es obligatorio")
        String username,
        @NotBlank(message = "password es obligatorio")
        String password
) {
}
