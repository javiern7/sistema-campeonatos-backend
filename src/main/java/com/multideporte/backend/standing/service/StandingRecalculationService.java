package com.multideporte.backend.standing.service;

import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;

public interface StandingRecalculationService {

    StandingRecalculationResponse recalculate(StandingRecalculateRequest request);
}
