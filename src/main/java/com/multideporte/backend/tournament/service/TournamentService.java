package com.multideporte.backend.tournament.service;

import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentStatusTransitionRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentService {

    TournamentResponse create(TournamentCreateRequest request);

    TournamentResponse getById(Long id);

    Page<TournamentResponse> getAll(String name, Long sportId, TournamentStatus status, Pageable pageable);

    TournamentResponse update(Long id, TournamentUpdateRequest request);

    TournamentResponse transitionStatus(Long id, TournamentStatusTransitionRequest request);

    TournamentKnockoutProgressionResponse progressToKnockout(Long id);

    void delete(Long id);
}
