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
import com.multideporte.backend.stagegroup.service.StageGroupService;
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

    @Override
    @Transactional
    public StageGroupResponse create(StageGroupCreateRequest request) {
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

        if (matchGameRepository.existsByGroupId(id) || standingRepository.existsByGroupId(id)) {
            throw new BusinessException("No se puede eliminar el grupo porque ya tiene partidos o standings asociados");
        }

        stageGroupRepository.delete(entity);
    }

    private StageGroup findGroup(Long id) {
        return stageGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StageGroup no encontrado con id: " + id));
    }
}
