package com.multideporte.backend.standing.service;

import com.multideporte.backend.standing.dto.request.StandingCreateRequest;
import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.request.StandingUpdateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;
import com.multideporte.backend.standing.dto.response.StandingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StandingService {

    StandingResponse create(StandingCreateRequest request);

    StandingResponse getById(Long id);

    Page<StandingResponse> getAll(Long tournamentId, Long stageId, Long groupId, Long tournamentTeamId, Pageable pageable);

    StandingResponse update(Long id, StandingUpdateRequest request);

    void delete(Long id);

    StandingRecalculationResponse recalculate(StandingRecalculateRequest request);
}
