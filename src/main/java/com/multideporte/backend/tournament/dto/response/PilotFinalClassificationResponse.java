package com.multideporte.backend.tournament.dto.response;

import java.util.List;

public record PilotFinalClassificationResponse(Long tournamentId, Long stageId, List<Entry> entries) {
    public record Entry(Integer position, Long tournamentTeamId) { }
}