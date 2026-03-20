package com.multideporte.backend.player.service;

import com.multideporte.backend.player.dto.request.PlayerCreateRequest;
import com.multideporte.backend.player.dto.request.PlayerUpdateRequest;
import com.multideporte.backend.player.dto.response.PlayerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlayerService {

    PlayerResponse create(PlayerCreateRequest request);

    PlayerResponse getById(Long id);

    Page<PlayerResponse> getAll(
            String search,
            String documentType,
            String documentNumber,
            Boolean active,
            Pageable pageable
    );

    PlayerResponse update(Long id, PlayerUpdateRequest request);

    void delete(Long id);
}
