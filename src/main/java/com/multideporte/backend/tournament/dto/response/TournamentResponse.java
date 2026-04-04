package com.multideporte.backend.tournament.dto.response;

import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TournamentResponse(
        Long id,
        Long sportId,
        String name,
        String slug,
        String seasonName,
        TournamentFormat format,
        TournamentStatus status,
        TournamentOperationalCategory operationalCategory,
        boolean executiveReportingEligible,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime registrationOpenAt,
        OffsetDateTime registrationCloseAt,
        Integer maxTeams,
        Integer pointsWin,
        Integer pointsDraw,
        Integer pointsLoss,
        Long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
