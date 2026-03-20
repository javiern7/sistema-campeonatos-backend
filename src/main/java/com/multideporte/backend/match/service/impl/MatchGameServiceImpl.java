package com.multideporte.backend.match.service.impl;

import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.dto.request.MatchGameCreateRequest;
import com.multideporte.backend.match.dto.request.MatchGameUpdateRequest;
import com.multideporte.backend.match.dto.response.MatchGameResponse;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.mapper.MatchGameMapper;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.match.repository.MatchGameSpecifications;
import com.multideporte.backend.match.service.MatchGameService;
import com.multideporte.backend.match.validation.MatchGameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchGameServiceImpl implements MatchGameService {

    private final MatchGameRepository matchGameRepository;
    private final MatchGameMapper matchGameMapper;
    private final MatchGameValidator matchGameValidator;

    @Override
    @Transactional
    public MatchGameResponse create(MatchGameCreateRequest request) {
        matchGameValidator.validateForCreate(
                request.tournamentId(),
                request.stageId(),
                request.groupId(),
                request.homeTournamentTeamId(),
                request.awayTournamentTeamId(),
                request.status(),
                request.homeScore(),
                request.awayScore(),
                request.winnerTournamentTeamId()
        );

        MatchGame entity = matchGameMapper.toEntity(request);
        MatchGame saved = matchGameRepository.save(entity);
        return matchGameMapper.toResponse(saved);
    }

    @Override
    public MatchGameResponse getById(Long id) {
        return matchGameMapper.toResponse(findMatch(id));
    }

    @Override
    public Page<MatchGameResponse> getAll(Long tournamentId, Long stageId, Long groupId, MatchGameStatus status, Pageable pageable) {
        return matchGameRepository.findAll(MatchGameSpecifications.byFilters(tournamentId, stageId, groupId, status), pageable)
                .map(matchGameMapper::toResponse);
    }

    @Override
    @Transactional
    public MatchGameResponse update(Long id, MatchGameUpdateRequest request) {
        MatchGame entity = findMatch(id);
        matchGameValidator.validateForUpdate(
                entity,
                request.stageId(),
                request.groupId(),
                request.homeTournamentTeamId(),
                request.awayTournamentTeamId(),
                request.status(),
                request.homeScore(),
                request.awayScore(),
                request.winnerTournamentTeamId()
        );

        matchGameMapper.updateEntity(entity, request);
        MatchGame saved = matchGameRepository.save(entity);
        return matchGameMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        matchGameRepository.delete(findMatch(id));
    }

    private MatchGame findMatch(Long id) {
        return matchGameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MatchGame no encontrado con id: " + id));
    }
}
