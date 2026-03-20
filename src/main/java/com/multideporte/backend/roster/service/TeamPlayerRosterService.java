package com.multideporte.backend.roster.service;

import com.multideporte.backend.roster.dto.request.TeamPlayerRosterCreateRequest;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterUpdateRequest;
import com.multideporte.backend.roster.dto.response.TeamPlayerRosterResponse;
import com.multideporte.backend.roster.entity.RosterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamPlayerRosterService {

    TeamPlayerRosterResponse create(TeamPlayerRosterCreateRequest request);

    TeamPlayerRosterResponse getById(Long id);

    Page<TeamPlayerRosterResponse> getAll(Long tournamentTeamId, Long playerId, RosterStatus rosterStatus, Pageable pageable);

    TeamPlayerRosterResponse update(Long id, TeamPlayerRosterUpdateRequest request);

    void delete(Long id);
}
