package com.multideporte.backend.team.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.team.dto.request.TeamCreateRequest;
import com.multideporte.backend.team.dto.request.TeamUpdateRequest;
import com.multideporte.backend.team.dto.response.TeamResponse;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.mapper.TeamMapper;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.team.repository.TeamSpecifications;
import com.multideporte.backend.team.repository.TeamTournamentRepository;
import com.multideporte.backend.team.service.TeamService;
import com.multideporte.backend.team.validation.TeamValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamTournamentRepository teamTournamentRepository;
    private final TeamMapper teamMapper;
    private final TeamValidator teamValidator;

    @Override
    @Transactional
    public TeamResponse create(TeamCreateRequest request) {
        teamValidator.validateForCreate(request.code(), request.primaryColor(), request.secondaryColor());

        Team entity = teamMapper.toEntity(request);
        entity.setCode(teamValidator.normalizeCode(request.code()));

        Team saved = teamRepository.save(entity);
        return teamMapper.toResponse(saved);
    }

    @Override
    public TeamResponse getById(Long id) {
        return teamMapper.toResponse(findTeam(id));
    }

    @Override
    public Page<TeamResponse> getAll(String name, String code, Boolean active, Pageable pageable) {
        return teamRepository.findAll(TeamSpecifications.byFilters(name, code, active), pageable)
                .map(teamMapper::toResponse);
    }

    @Override
    @Transactional
    public TeamResponse update(Long id, TeamUpdateRequest request) {
        Team entity = findTeam(id);
        teamValidator.validateForUpdate(entity, request.code(), request.primaryColor(), request.secondaryColor());

        teamMapper.updateEntity(entity, request);
        entity.setCode(teamValidator.normalizeCode(request.code()));

        Team saved = teamRepository.save(entity);
        return teamMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Team entity = findTeam(id);

        if (teamTournamentRepository.existsByTeamId(id)) {
            throw new BusinessException("No se puede eliminar el equipo porque ya esta asociado a un torneo");
        }

        teamRepository.delete(entity);
    }

    private Team findTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team no encontrado con id: " + id));
    }
}
