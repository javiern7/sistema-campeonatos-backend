package com.multideporte.backend.match.service;

import com.multideporte.backend.match.dto.request.MatchGameCreateRequest;
import com.multideporte.backend.match.dto.request.MatchGameUpdateRequest;
import com.multideporte.backend.match.dto.response.MatchGameResponse;
import com.multideporte.backend.match.entity.MatchGameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchGameService {

    MatchGameResponse create(MatchGameCreateRequest request);

    MatchGameResponse getById(Long id);

    Page<MatchGameResponse> getAll(Long tournamentId, Long stageId, Long groupId, MatchGameStatus status, Pageable pageable);

    MatchGameResponse update(Long id, MatchGameUpdateRequest request);

    void delete(Long id);
}
