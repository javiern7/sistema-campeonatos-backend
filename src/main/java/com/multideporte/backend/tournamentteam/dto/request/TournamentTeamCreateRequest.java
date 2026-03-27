package com.multideporte.backend.tournamentteam.dto.request;

import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TournamentTeamCreateRequest(
        @NotNull(message = "tournamentId es obligatorio")
        Long tournamentId,

        @NotNull(message = "teamId es obligatorio")
        Long teamId,

        @NotNull(message = "registrationStatus es obligatorio")
        TournamentTeamRegistrationStatus registrationStatus,

        @Min(value = 0, message = "seedNumber debe ser mayor o igual a 0")
        Integer seedNumber,

        @Min(value = 0, message = "groupDrawPosition debe ser mayor o igual a 0")
        Integer groupDrawPosition
) {
}
