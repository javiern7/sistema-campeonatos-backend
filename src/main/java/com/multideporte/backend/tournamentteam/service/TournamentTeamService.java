package com.multideporte.backend.tournamentteam.service;

import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamCreateRequest;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamUpdateRequest;
import com.multideporte.backend.tournamentteam.dto.response.TournamentTeamResponse;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentTeamService {

    TournamentTeamResponse create(TournamentTeamCreateRequest request);

    TournamentTeamResponse getById(Long id);

    Page<TournamentTeamResponse> getAll(
            Long tournamentId,
            Long teamId,
            TournamentTeamRegistrationStatus registrationStatus,
            Pageable pageable
    );

    TournamentTeamResponse update(Long id, TournamentTeamUpdateRequest request);

    void delete(Long id);
}
