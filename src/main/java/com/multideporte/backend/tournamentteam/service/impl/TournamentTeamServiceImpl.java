package com.multideporte.backend.tournamentteam.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamCreateRequest;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamUpdateRequest;
import com.multideporte.backend.tournamentteam.dto.response.TournamentTeamResponse;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.mapper.TournamentTeamMapper;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRosterRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamSpecifications;
import com.multideporte.backend.tournamentteam.service.TournamentTeamService;
import com.multideporte.backend.tournamentteam.validation.TournamentTeamValidator;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private final TournamentTeamRepository tournamentTeamRepository;
    private final TournamentTeamRosterRepository tournamentTeamRosterRepository;
    private final TournamentTeamMapper tournamentTeamMapper;
    private final TournamentTeamValidator tournamentTeamValidator;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;

    @Override
    @Transactional
    public TournamentTeamResponse create(TournamentTeamCreateRequest request) {
        tournamentTeamValidator.validateForCreate(
                request.tournamentId(),
                request.teamId(),
                request.registrationStatus(),
                request.seedNumber(),
                request.groupDrawPosition()
        );

        TournamentTeam entity = tournamentTeamMapper.toEntity(request);
        entity.setJoinedAt(OffsetDateTime.now());

        TournamentTeam saved = tournamentTeamRepository.save(entity);
        return tournamentTeamMapper.toResponse(saved);
    }

    @Override
    public TournamentTeamResponse getById(Long id) {
        return tournamentTeamMapper.toResponse(findTournamentTeam(id));
    }

    @Override
    public Page<TournamentTeamResponse> getAll(
            Long tournamentId,
            Long teamId,
            TournamentTeamRegistrationStatus registrationStatus,
            Pageable pageable
    ) {
        return tournamentTeamRepository.findAll(
                        TournamentTeamSpecifications.byFilters(tournamentId, teamId, registrationStatus),
                        pageable
                )
                .map(tournamentTeamMapper::toResponse);
    }

    @Override
    @Transactional
    public TournamentTeamResponse update(Long id, TournamentTeamUpdateRequest request) {
        TournamentTeam entity = findTournamentTeam(id);
        tournamentTeamValidator.validateForUpdate(
                entity,
                request.registrationStatus(),
                request.seedNumber(),
                request.groupDrawPosition()
        );
        assertRegistrationStatusChangeIsAllowed(entity, request.registrationStatus());

        tournamentTeamMapper.updateEntity(entity, request);
        TournamentTeam saved = tournamentTeamRepository.save(entity);
        return tournamentTeamMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TournamentTeam entity = findTournamentTeam(id);

        if (tournamentTeamRosterRepository.existsByTournamentTeamId(id)) {
            throw new BusinessException("No se puede eliminar la inscripcion porque ya tiene jugadores en roster");
        }

        if (matchGameRepository.existsByHomeTournamentTeamIdOrAwayTournamentTeamIdOrWinnerTournamentTeamId(id, id, id)
                || standingRepository.existsByTournamentTeamId(id)) {
            throw new BusinessException("No se puede eliminar la inscripcion porque ya tiene partidos o standings asociados");
        }

        tournamentTeamRepository.delete(entity);
    }

    private TournamentTeam findTournamentTeam(Long id) {
        return tournamentTeamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentTeam no encontrado con id: " + id));
    }

    private void assertRegistrationStatusChangeIsAllowed(
            TournamentTeam current,
            TournamentTeamRegistrationStatus requestedStatus
    ) {
        if (current.getRegistrationStatus() == requestedStatus) {
            return;
        }

        if (requestedStatus == TournamentTeamRegistrationStatus.APPROVED) {
            return;
        }

        boolean hasRelatedOperation = tournamentTeamRosterRepository.existsByTournamentTeamId(current.getId())
                || matchGameRepository.existsByHomeTournamentTeamIdOrAwayTournamentTeamIdOrWinnerTournamentTeamId(
                current.getId(),
                current.getId(),
                current.getId()
        )
                || standingRepository.existsByTournamentTeamId(current.getId());

        if (hasRelatedOperation) {
            throw new BusinessException(
                    "No se puede cambiar la inscripcion a un estado no APPROVED cuando ya tiene roster, partidos o standings asociados"
            );
        }
    }
}
