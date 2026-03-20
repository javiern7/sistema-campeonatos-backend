package com.multideporte.backend.team.service;

import com.multideporte.backend.team.dto.request.TeamCreateRequest;
import com.multideporte.backend.team.dto.request.TeamUpdateRequest;
import com.multideporte.backend.team.dto.response.TeamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamService {

    TeamResponse create(TeamCreateRequest request);

    TeamResponse getById(Long id);

    Page<TeamResponse> getAll(String name, String code, Boolean active, Pageable pageable);

    TeamResponse update(Long id, TeamUpdateRequest request);

    void delete(Long id);
}
