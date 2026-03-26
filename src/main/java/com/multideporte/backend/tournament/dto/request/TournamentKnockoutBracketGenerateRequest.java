package com.multideporte.backend.tournament.dto.request;

import jakarta.validation.constraints.Min;

public record TournamentKnockoutBracketGenerateRequest(
        KnockoutSeedingStrategy seedingStrategy,

        @Min(value = 1, message = "roundNumber debe ser mayor a 0")
        Integer roundNumber,

        @Min(value = 1, message = "matchdayNumber debe ser mayor a 0")
        Integer matchdayNumber
) {
}
