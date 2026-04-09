package com.multideporte.backend.discipline.dto.request;

import com.multideporte.backend.discipline.entity.DisciplinaryIncidentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplinaryIncidentCreateRequest(
        @NotNull(message = "tournamentTeamId es obligatorio")
        Long tournamentTeamId,

        @NotNull(message = "playerId es obligatorio")
        Long playerId,

        @NotNull(message = "incidentType es obligatorio")
        DisciplinaryIncidentType incidentType,

        @Min(value = 0, message = "incidentMinute no puede ser negativo")
        Integer incidentMinute,

        @Size(max = 500, message = "notes no puede superar 500 caracteres")
        String notes
) {
}
