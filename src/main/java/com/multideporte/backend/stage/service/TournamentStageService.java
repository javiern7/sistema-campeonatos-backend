package com.multideporte.backend.stage.service;

import com.multideporte.backend.stage.dto.request.TournamentStageCreateRequest;
import com.multideporte.backend.stage.dto.request.TournamentStageUpdateRequest;
import com.multideporte.backend.stage.dto.response.TournamentStageResponse;
import com.multideporte.backend.stage.entity.TournamentStageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentStageService {

    TournamentStageResponse create(TournamentStageCreateRequest request);

    TournamentStageResponse getById(Long id);

    Page<TournamentStageResponse> getAll(Long tournamentId, TournamentStageType stageType, Boolean active, Pageable pageable);

    TournamentStageResponse update(Long id, TournamentStageUpdateRequest request);

    void delete(Long id);
}
