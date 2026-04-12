package com.multideporte.backend.matchevent.dto.request;

import jakarta.validation.constraints.Size;

public record MatchEventAnnulRequest(
        @Size(max = 500) String notes
) {
}
