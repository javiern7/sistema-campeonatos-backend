package com.multideporte.backend.tournament.service;

import com.multideporte.backend.tournament.dto.request.TournamentKnockoutBracketGenerateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutBracketResponse;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.entity.Tournament;

public interface TournamentStageProgressionService {

    TournamentKnockoutProgressionResponse progressGroupsThenKnockout(Tournament tournament);

    TournamentKnockoutBracketResponse generateKnockoutBracket(
            Tournament tournament,
            TournamentKnockoutBracketGenerateRequest request
    );

    void assertMatchStageCanBeManaged(
            Tournament tournament,
            Long stageId,
            Long groupId,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    );

    void assertStandingsCanBeRecalculated(Tournament tournament, Long stageId, Long groupId);
}
