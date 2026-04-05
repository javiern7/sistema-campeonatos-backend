package com.multideporte.backend.security.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(
        @NotBlank(message = "refreshToken es obligatorio")
        String refreshToken
) {
}
