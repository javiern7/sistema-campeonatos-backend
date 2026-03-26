package com.multideporte.backend.stagegroup.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stagegroup.dto.request.StageGroupCreateRequest;
import com.multideporte.backend.stagegroup.dto.request.StageGroupUpdateRequest;
import com.multideporte.backend.stagegroup.dto.response.StageGroupResponse;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.mapper.StageGroupMapper;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.stagegroup.repository.StageGroupSpecifications;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.service.StageGroupService;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.service.TournamentLifecycleGuardService;
import com.multideporte.backend.stagegroup.validation.StageGroupValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageGroupServiceImpl implements StageGroupService {

    private final StageGroupRepository stageGroupRepository;
    private final StageGroupMapper stageGroupMapper;
    private final StageGroupValidator stageGroupValidator;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentLifecycleGuardService tournamentLifecycleGuardService;

    @Override
    @Transactional
    public StageGroupResponse create(StageGroupCreateRequest request) {
        TournamentStage stage = loadStage(request.stageId());
        tournamentLifecycleGuardService.assertStructureCanBeModified(loadTournament(stage.getTournamentId()));
        stageGroupValidator.validateForCreate(request.stageId(), request.code(), request.sequenceOrder());

        StageGroup entity = stageGroupMapper.toEntity(request);
        entity.setCode(stageGroupValidator.normalizeCode(request.code()));

        StageGroup saved = stageGroupRepository.save(entity);
        return stageGroupMapper.toResponse(saved);
    }

    @Override
    public StageGroupResponse getById(Long id) {
        return stageGroupMapper.toResponse(findGroup(id));
    }

    @Override
    public Page<StageGroupResponse> getAll(Long stageId, String code, Pageable pageable) {
        return stageGroupRepository.findAll(StageGroupSpecifications.byFilters(stageId, code), pageable)
                .map(stageGroupMapper::toResponse);
    }

    @Override
    @Transactional
    public StageGroupResponse update(Long id, StageGroupUpdateRequest request) {
        StageGroup entity = findGroup(id);
        TournamentStage stage = loadStage(entity.getStageId());
        tournamentLifecycleGuardService.assertStructureCanBeModified(loadTournament(stage.getTournamentId()));
        stageGroupValidator.validateForUpdate(entity, request.code(), request.sequenceOrder());

        stageGroupMapper.updateEntity(entity, request);
        entity.setCode(stageGroupValidator.normalizeCode(request.code()));

        StageGroup saved = stageGroupRepository.save(entity);
        return stageGroupMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        StageGroup entity = findGroup(id);
        TournamentStage stage = loadStage(entity.getStageId());
        tournamentLifecycleGuardService.assertStructureCanBeModified(loadTournament(stage.getTournamentId()));

        if (matchGameRepository.existsByGroupId(id) || standingRepository.existsByGroupId(id)) {
            throw new BusinessException("No se puede eliminar el grupo porque ya tiene partidos o standings asociados");
        }

        stageGroupRepository.delete(entity);
    }

    private StageGroup findGroup(Long id) {
        return stageGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StageGroup no encontrado con id: " + id));
    }

    private TournamentStage loadStage(Long stageId) {
        return tournamentStageRepository.findById(stageId)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentStage no encontrado con id: " + stageId));
    }

    private Tournament loadTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament no encontrado con id: " + tournamentId));
    }
}
