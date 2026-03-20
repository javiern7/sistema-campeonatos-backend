package com.multideporte.backend.standing.dto.request;

import jakarta.validation.constraints.NotNull;

public record StandingRecalculateRequest(
        @NotNull(message = "tournamentId es obligatorio")
        Long tournamentId,
        Long stageId,
        Long groupId
) {
}
