package com.multideporte.backend.tournament.service;

import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.entity.Tournament;

public interface TournamentStageProgressionService {

    TournamentKnockoutProgressionResponse progressGroupsThenKnockout(Tournament tournament);

    void assertMatchStageCanBeManaged(
            Tournament tournament,
            Long stageId,
            Long groupId,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    );

    void assertStandingsCanBeRecalculated(Tournament tournament, Long stageId, Long groupId);
}
