package com.multideporte.backend.discipline.dto.request;

import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplinarySanctionCreateRequest(
        @NotNull(message = "sanctionType es obligatorio")
        DisciplinarySanctionType sanctionType,

        @Min(value = 0, message = "matchesToServe no puede ser negativo")
        Integer matchesToServe,

        @Size(max = 500, message = "notes no puede superar 500 caracteres")
        String notes
) {
}
