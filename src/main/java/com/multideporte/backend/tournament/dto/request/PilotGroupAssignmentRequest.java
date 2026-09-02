package com.multideporte.backend.tournament.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PilotGroupAssignmentRequest(
        @NotNull @Size(min = 4, max = 4) List<Long> tournamentTeamIds
) {
}
