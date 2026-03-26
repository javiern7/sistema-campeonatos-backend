package com.multideporte.backend.stage.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.dto.request.TournamentStageCreateRequest;
import com.multideporte.backend.stage.dto.request.TournamentStageUpdateRequest;
import com.multideporte.backend.stage.dto.response.TournamentStageResponse;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.mapper.TournamentStageMapper;
import com.multideporte.backend.stage.repository.TournamentStageGroupRepository;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stage.repository.TournamentStageSpecifications;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.stage.service.TournamentStageService;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.service.TournamentLifecycleGuardService;
import com.multideporte.backend.stage.validation.TournamentStageValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentStageServiceImpl implements TournamentStageService {

    private final TournamentStageRepository tournamentStageRepository;
    private final TournamentStageGroupRepository tournamentStageGroupRepository;
    private final TournamentStageMapper tournamentStageMapper;
    private final TournamentStageValidator tournamentStageValidator;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentLifecycleGuardService tournamentLifecycleGuardService;

    @Override
    @Transactional
    public TournamentStageResponse create(TournamentStageCreateRequest request) {
        Tournament tournament = loadTournament(request.tournamentId());
        tournamentLifecycleGuardService.assertStructureCanBeModified(tournament);
        tournamentStageValidator.validateForCreate(
                request.tournamentId(),
                request.stageType(),
                request.sequenceOrder(),
                request.legs(),
                request.roundTrip(),
                request.active()
        );

        TournamentStage entity = tournamentStageMapper.toEntity(request);
        TournamentStage saved = tournamentStageRepository.save(entity);
        return tournamentStageMapper.toResponse(saved);
    }

    @Override
    public TournamentStageResponse getById(Long id) {
        return tournamentStageMapper.toResponse(findStage(id));
    }

    @Override
    public Page<TournamentStageResponse> getAll(Long tournamentId, TournamentStageType stageType, Boolean active, Pageable pageable) {
        return tournamentStageRepository.findAll(TournamentStageSpecifications.byFilters(tournamentId, stageType, active), pageable)
                .map(tournamentStageMapper::toResponse);
    }

    @Override
    @Transactional
    public TournamentStageResponse update(Long id, TournamentStageUpdateRequest request) {
        TournamentStage entity = findStage(id);
        tournamentLifecycleGuardService.assertStructureCanBeModified(loadTournament(entity.getTournamentId()));
        tournamentStageValidator.validateForUpdate(
                entity,
                request.stageType(),
                request.sequenceOrder(),
                request.legs(),
                request.roundTrip(),
                request.active()
        );

        tournamentStageMapper.updateEntity(entity, request);
        TournamentStage saved = tournamentStageRepository.save(entity);
        return tournamentStageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TournamentStage entity = findStage(id);
        tournamentLifecycleGuardService.assertStructureCanBeModified(loadTournament(entity.getTournamentId()));

        if (tournamentStageGroupRepository.existsByStageId(id)) {
            throw new BusinessException("No se puede eliminar la etapa porque ya tiene grupos asociados");
        }

        if (matchGameRepository.existsByStageId(id) || standingRepository.existsByStageId(id)) {
            throw new BusinessException("No se puede eliminar la etapa porque ya tiene partidos o standings asociados");
        }

        tournamentStageRepository.delete(entity);
    }

    private TournamentStage findStage(Long id) {
        return tournamentStageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentStage no encontrado con id: " + id));
    }

    private Tournament loadTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament no encontrado con id: " + tournamentId));
    }
}
