package com.multideporte.backend.standing.service.impl;

import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.standing.dto.request.StandingCreateRequest;
import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.request.StandingUpdateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;
import com.multideporte.backend.standing.dto.response.StandingResponse;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.mapper.StandingMapper;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.standing.repository.StandingSpecifications;
import com.multideporte.backend.standing.service.StandingService;
import com.multideporte.backend.standing.validation.StandingValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingServiceImpl implements StandingService {

    private final StandingRepository standingRepository;
    private final StandingMapper standingMapper;
    private final StandingValidator standingValidator;
    private final com.multideporte.backend.standing.service.StandingRecalculationService standingRecalculationService;

    @Override
    @Transactional
    public StandingResponse create(StandingCreateRequest request) {
        Standing entity = standingMapper.toEntity(request);
        standingValidator.validateForCreate(entity);

        Standing saved = standingRepository.save(entity);
        return standingMapper.toResponse(saved);
    }

    @Override
    public StandingResponse getById(Long id) {
        return standingMapper.toResponse(findStanding(id));
    }

    @Override
    public Page<StandingResponse> getAll(Long tournamentId, Long stageId, Long groupId, Long tournamentTeamId, Pageable pageable) {
        return standingRepository.findAll(
                        StandingSpecifications.byFilters(tournamentId, stageId, groupId, tournamentTeamId),
                        pageable
                )
                .map(standingMapper::toResponse);
    }

    @Override
    @Transactional
    public StandingResponse update(Long id, StandingUpdateRequest request) {
        Standing entity = findStanding(id);
        standingMapper.updateEntity(entity, request);
        standingValidator.validateForUpdate(entity, entity);

        Standing saved = standingRepository.save(entity);
        return standingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        standingRepository.delete(findStanding(id));
    }

    @Override
    @Transactional
    public StandingRecalculationResponse recalculate(StandingRecalculateRequest request) {
        return standingRecalculationService.recalculate(request);
    }

    private Standing findStanding(Long id) {
        return standingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Standing no encontrado con id: " + id));
    }
}
