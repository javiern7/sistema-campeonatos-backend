package com.multideporte.backend.roster.service.impl;

import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterCreateRequest;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterUpdateRequest;
import com.multideporte.backend.roster.dto.response.TeamPlayerRosterResponse;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import com.multideporte.backend.roster.mapper.TeamPlayerRosterMapper;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.roster.repository.TeamPlayerRosterSpecifications;
import com.multideporte.backend.roster.service.TeamPlayerRosterService;
import com.multideporte.backend.roster.validation.TeamPlayerRosterValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamPlayerRosterServiceImpl implements TeamPlayerRosterService {

    private final TeamPlayerRosterRepository teamPlayerRosterRepository;
    private final TeamPlayerRosterMapper teamPlayerRosterMapper;
    private final TeamPlayerRosterValidator teamPlayerRosterValidator;

    @Override
    @Transactional
    public TeamPlayerRosterResponse create(TeamPlayerRosterCreateRequest request) {
        teamPlayerRosterValidator.validateForCreate(
                request.tournamentTeamId(),
                request.playerId(),
                request.jerseyNumber(),
                request.captain(),
                request.rosterStatus(),
                request.startDate(),
                request.endDate()
        );

        TeamPlayerRoster entity = teamPlayerRosterMapper.toEntity(request);
        TeamPlayerRoster saved = teamPlayerRosterRepository.save(entity);
        return teamPlayerRosterMapper.toResponse(saved);
    }

    @Override
    public TeamPlayerRosterResponse getById(Long id) {
        return teamPlayerRosterMapper.toResponse(findRoster(id));
    }

    @Override
    public Page<TeamPlayerRosterResponse> getAll(Long tournamentTeamId, Long playerId, RosterStatus rosterStatus, Pageable pageable) {
        return teamPlayerRosterRepository.findAll(
                        TeamPlayerRosterSpecifications.byFilters(tournamentTeamId, playerId, rosterStatus),
                        pageable
                )
                .map(teamPlayerRosterMapper::toResponse);
    }

    @Override
    @Transactional
    public TeamPlayerRosterResponse update(Long id, TeamPlayerRosterUpdateRequest request) {
        TeamPlayerRoster entity = findRoster(id);
        teamPlayerRosterValidator.validateForUpdate(
                entity,
                request.jerseyNumber(),
                request.captain(),
                request.rosterStatus(),
                request.startDate(),
                request.endDate()
        );

        teamPlayerRosterMapper.updateEntity(entity, request);
        TeamPlayerRoster saved = teamPlayerRosterRepository.save(entity);
        return teamPlayerRosterMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        teamPlayerRosterRepository.delete(findRoster(id));
    }

    private TeamPlayerRoster findRoster(Long id) {
        return teamPlayerRosterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roster no encontrado con id: " + id));
    }
}
