package com.multideporte.backend.tournament.dto.request;

import com.multideporte.backend.tournament.entity.TournamentStatus;
import jakarta.validation.constraints.NotNull;

public record TournamentStatusTransitionRequest(
        @NotNull(message = "targetStatus es obligatorio")
        TournamentStatus targetStatus
) {
}
