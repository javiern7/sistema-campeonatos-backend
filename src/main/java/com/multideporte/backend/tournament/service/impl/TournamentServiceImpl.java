package com.multideporte.backend.tournament.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.user.CurrentUserService;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentResponse;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.mapper.TournamentMapper;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.repository.TournamentSpecifications;
import com.multideporte.backend.tournament.repository.TournamentStageRefRepository;
import com.multideporte.backend.tournament.repository.TournamentTeamRefRepository;
import com.multideporte.backend.tournament.service.TournamentService;
import com.multideporte.backend.tournament.validation.TournamentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRefRepository tournamentStageRepository;
    private final TournamentTeamRefRepository tournamentTeamRepository;
    private final TournamentMapper tournamentMapper;
    private final TournamentValidator tournamentValidator;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TournamentResponse create(TournamentCreateRequest request) {
        tournamentValidator.validateForCreate(request);

        Tournament entity = tournamentMapper.toEntity(request);
        entity.setSlug(tournamentValidator.buildSlug(request.name(), request.seasonName()));
        entity.setCreatedByUserId(currentUserService.requireCurrentUserId());

        Tournament saved = tournamentRepository.save(entity);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    public TournamentResponse getById(Long id) {
        return tournamentMapper.toResponse(findTournament(id));
    }

    @Override
    public Page<TournamentResponse> getAll(String name, Long sportId, TournamentStatus status, Pageable pageable) {
        return tournamentRepository.findAll(TournamentSpecifications.byFilters(name, sportId, status), pageable)
                .map(tournamentMapper::toResponse);
    }

    @Override
    @Transactional
    public TournamentResponse update(Long id, TournamentUpdateRequest request) {
        Tournament entity = findTournament(id);
        tournamentValidator.validateForUpdate(entity, request);

        tournamentMapper.updateEntity(entity, request);
        entity.setSlug(tournamentValidator.buildSlug(request.name(), request.seasonName()));

        Tournament saved = tournamentRepository.save(entity);
        return tournamentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tournament entity = findTournament(id);

        if (tournamentStageRepository.existsByTournamentId(id) || tournamentTeamRepository.existsByTournamentId(id)) {
            throw new BusinessException("No se puede eliminar el torneo porque ya tiene etapas o equipos asociados");
        }

        if (entity.getStatus() == TournamentStatus.IN_PROGRESS || entity.getStatus() == TournamentStatus.FINISHED) {
            throw new BusinessException("No se puede eliminar un torneo en progreso o finalizado");
        }

        tournamentRepository.delete(entity);
    }

    private Tournament findTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament no encontrado con id: " + id));
    }
}
