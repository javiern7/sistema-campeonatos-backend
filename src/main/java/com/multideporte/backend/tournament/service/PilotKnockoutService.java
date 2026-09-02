package com.multideporte.backend.tournament.service;

import com.multideporte.backend.tournament.dto.response.PilotKnockoutResponse;
import com.multideporte.backend.tournament.dto.response.PilotFinalClassificationResponse;

public interface PilotKnockoutService {
    PilotKnockoutResponse createSemifinals(Long tournamentId, Long groupStageId, Long knockoutStageId);

    PilotKnockoutResponse createFinalAndThirdPlace(Long tournamentId, Long knockoutStageId);

    PilotFinalClassificationResponse finalClassification(Long tournamentId, Long knockoutStageId);
}
