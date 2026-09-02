package com.multideporte.backend.match.service;

import com.multideporte.backend.match.entity.MatchResolutionMethod;

public record MatchResultResolution(
        Long winnerTournamentTeamId,
        MatchResolutionMethod resolutionMethod
) {
}
