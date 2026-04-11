package com.multideporte.backend.sport.service;

import com.multideporte.backend.sport.dto.request.SportCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionUpdateRequest;
import com.multideporte.backend.sport.dto.request.SportUpdateRequest;
import com.multideporte.backend.sport.dto.response.CompetitionFormatResponse;
import com.multideporte.backend.sport.dto.response.SportPositionResponse;
import com.multideporte.backend.sport.dto.response.SportResponse;
import java.util.List;

public interface SportService {

    SportResponse create(SportCreateRequest request);

    SportResponse getById(Long id);

    List<SportResponse> getAll(Boolean activeOnly);

    SportResponse update(Long id, SportUpdateRequest request);

    void delete(Long id);

    List<SportPositionResponse> getPositions(Long sportId, Boolean activeOnly);

    SportPositionResponse createPosition(Long sportId, SportPositionCreateRequest request);

    SportPositionResponse updatePosition(Long sportId, Long positionId, SportPositionUpdateRequest request);

    void deletePosition(Long sportId, Long positionId);

    List<CompetitionFormatResponse> getCompetitionFormats();
}
