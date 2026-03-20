package com.multideporte.backend.tournamentteam.dto.response;

import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import java.time.OffsetDateTime;

public record TournamentTeamResponse(
        Long id,
        Long tournamentId,
        Long teamId,
        TournamentTeamRegistrationStatus registrationStatus,
        Integer seedNumber,
        Integer groupDrawPosition,
        OffsetDateTime joinedAt
) {
}
