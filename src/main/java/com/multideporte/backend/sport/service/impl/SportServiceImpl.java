package com.multideporte.backend.sport.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.sport.dto.request.SportCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionUpdateRequest;
import com.multideporte.backend.sport.dto.request.SportUpdateRequest;
import com.multideporte.backend.sport.dto.response.CompetitionFormatResponse;
import com.multideporte.backend.sport.dto.response.SportPositionResponse;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.entity.SportPosition;
import com.multideporte.backend.sport.mapper.SportMapper;
import com.multideporte.backend.sport.repository.SportPositionRepository;
import com.multideporte.backend.sport.repository.SportRepository;
import com.multideporte.backend.sport.service.SportService;
import com.multideporte.backend.sport.validation.SportValidator;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SportServiceImpl implements SportService {

    private final SportRepository sportRepository;
    private final SportPositionRepository sportPositionRepository;
    private final TournamentRepository tournamentRepository;
    private final SportMapper sportMapper;
    private final SportValidator sportValidator;

    @Override
    @Transactional
    public SportResponse create(SportCreateRequest request) {
        sportValidator.validateSportForCreate(request.code(), request.scoreLabel());

        Sport entity = sportMapper.toEntity(request);
        normalizeSport(entity);

        return sportMapper.toResponse(sportRepository.save(entity));
    }

    @Override
    public SportResponse getById(Long id) {
        return sportMapper.toResponse(findSport(id));
    }

    @Override
    public List<SportResponse> getAll(Boolean activeOnly) {
        return sportRepository.findAll()
                .stream()
                .filter(sport -> !Boolean.TRUE.equals(activeOnly) || Boolean.TRUE.equals(sport.getActive()))
                .map(sportMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SportResponse update(Long id, SportUpdateRequest request) {
        Sport entity = findSport(id);
        sportValidator.validateSportForUpdate(entity, request.code(), request.scoreLabel());

        sportMapper.updateEntity(entity, request);
        normalizeSport(entity);

        return sportMapper.toResponse(sportRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Sport entity = findSport(id);

        if (tournamentRepository.existsBySportId(id)) {
            throw new BusinessException("No se puede eliminar el deporte porque ya tiene torneos asociados");
        }
        if (sportPositionRepository.existsBySportId(id)) {
            throw new BusinessException("No se puede eliminar el deporte porque ya tiene posiciones configuradas");
        }

        sportRepository.delete(entity);
    }

    @Override
    public List<SportPositionResponse> getPositions(Long sportId, Boolean activeOnly) {
        findSport(sportId);
        List<SportPosition> positions = Boolean.TRUE.equals(activeOnly)
                ? sportPositionRepository.findBySportIdAndActiveOrderByDisplayOrderAscNameAsc(sportId, true)
                : sportPositionRepository.findBySportIdOrderByDisplayOrderAscNameAsc(sportId);
        return positions.stream()
                .map(sportMapper::toPositionResponse)
                .toList();
    }

    @Override
    @Transactional
    public SportPositionResponse createPosition(Long sportId, SportPositionCreateRequest request) {
        Sport sport = findSport(sportId);
        sportValidator.validatePositionForCreate(sportId, request.code(), request.displayOrder());

        SportPosition entity = sportMapper.toPositionEntity(request);
        entity.setSport(sport);
        normalizePosition(entity);

        return sportMapper.toPositionResponse(sportPositionRepository.save(entity));
    }

    @Override
    @Transactional
    public SportPositionResponse updatePosition(Long sportId, Long positionId, SportPositionUpdateRequest request) {
        findSport(sportId);
        SportPosition entity = findPosition(sportId, positionId);
        sportValidator.validatePositionForUpdate(entity, request.code(), request.displayOrder());

        sportMapper.updatePositionEntity(entity, request);
        normalizePosition(entity);

        return sportMapper.toPositionResponse(sportPositionRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePosition(Long sportId, Long positionId) {
        findSport(sportId);
        sportPositionRepository.delete(findPosition(sportId, positionId));
    }

    @Override
    public List<CompetitionFormatResponse> getCompetitionFormats() {
        return Arrays.stream(TournamentFormat.values())
                .map(format -> new CompetitionFormatResponse(
                        format.name(),
                        formatName(format),
                        formatDescription(format)
                ))
                .toList();
    }

    private Sport findSport(Long id) {
        return sportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sport no encontrado con id: " + id));
    }

    private SportPosition findPosition(Long sportId, Long positionId) {
        return sportPositionRepository.findById(positionId)
                .filter(position -> position.getSport().getId().equals(sportId))
                .orElseThrow(() -> new ResourceNotFoundException("SportPosition no encontrada con id: " + positionId));
    }

    private void normalizeSport(Sport entity) {
        entity.setCode(sportValidator.normalizeCode(entity.getCode()));
        entity.setName(sportValidator.normalizeText(entity.getName()));
        entity.setScoreLabel(sportValidator.normalizeText(entity.getScoreLabel()));
    }

    private void normalizePosition(SportPosition entity) {
        entity.setCode(sportValidator.normalizeCode(entity.getCode()));
        entity.setName(sportValidator.normalizeText(entity.getName()));
    }

    private String formatName(TournamentFormat format) {
        return switch (format) {
            case LEAGUE -> "Liga";
            case GROUPS_THEN_KNOCKOUT -> "Grupos y eliminatoria";
            case KNOCKOUT -> "Eliminatoria";
        };
    }

    private String formatDescription(TournamentFormat format) {
        return switch (format) {
            case LEAGUE -> "Todos compiten en tabla de posiciones.";
            case GROUPS_THEN_KNOCKOUT -> "Fase de grupos con pase posterior a eliminatoria.";
            case KNOCKOUT -> "Llaves de eliminacion directa.";
        };
    }
}
