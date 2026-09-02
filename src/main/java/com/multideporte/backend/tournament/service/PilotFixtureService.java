package com.multideporte.backend.tournament.service;

import com.multideporte.backend.tournament.dto.request.PilotGroupAssignmentRequest;
import com.multideporte.backend.tournament.dto.response.PilotFixtureResponse;

public interface PilotFixtureService {
    void assignGroup(Long tournamentId, Long stageId, Long groupId, PilotGroupAssignmentRequest request);

    PilotFixtureResponse generate(Long tournamentId, Long stageId);
}
