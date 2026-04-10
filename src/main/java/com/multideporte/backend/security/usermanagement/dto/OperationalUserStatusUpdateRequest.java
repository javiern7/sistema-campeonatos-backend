package com.multideporte.backend.security.usermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record OperationalUserStatusUpdateRequest(
        @NotBlank(message = "status es obligatorio")
        String status,

        @NotBlank(message = "reason es obligatorio")
        String reason
) {
}
