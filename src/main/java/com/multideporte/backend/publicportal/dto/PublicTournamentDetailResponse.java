package com.multideporte.backend.publicportal.dto;

import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PublicTournamentDetailResponse(
        Long id,
        Long sportId,
        String sportName,
        String name,
        String slug,
        String seasonName,
        TournamentFormat format,
        TournamentStatus status,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime updatedAt,
        PublicReadModulesResponse modules
) {
}
