package com.multideporte.backend.roster.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.player.repository.PlayerRepository;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamPlayerRosterValidator {

    private final TournamentTeamRepository tournamentTeamRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerRosterRepository teamPlayerRosterRepository;

    public void validateForCreate(
            Long tournamentTeamId,
            Long playerId,
            Integer jerseyNumber,
            Boolean captain,
            RosterStatus rosterStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateReferences(tournamentTeamId, playerId);
        validateDates(startDate, endDate);
        validateJerseyNumber(jerseyNumber);
        validateCaptain(tournamentTeamId, captain, rosterStatus, null);

        if (teamPlayerRosterRepository.existsByTournamentTeamIdAndPlayerIdAndStartDate(tournamentTeamId, playerId, startDate)) {
            throw new BusinessException("Ya existe un registro de roster para ese jugador con la misma fecha de inicio");
        }

        if (rosterStatus == RosterStatus.ACTIVE
                && teamPlayerRosterRepository.existsByTournamentTeamIdAndPlayerIdAndRosterStatusAndEndDateIsNull(
                tournamentTeamId, playerId, RosterStatus.ACTIVE)) {
            throw new BusinessException("El jugador ya tiene un registro activo abierto en este roster");
        }
    }

    public void validateForUpdate(
            TeamPlayerRoster current,
            Integer jerseyNumber,
            Boolean captain,
            RosterStatus rosterStatus,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);
        validateJerseyNumber(jerseyNumber);
        validateCaptain(current.getTournamentTeamId(), captain, rosterStatus, current.getId());

        boolean duplicateHistory = teamPlayerRosterRepository.existsByTournamentTeamIdAndPlayerIdAndStartDate(
                current.getTournamentTeamId(),
                current.getPlayerId(),
                startDate
        );
        if (duplicateHistory && !startDate.equals(current.getStartDate())) {
            throw new BusinessException("Ya existe un registro de roster para ese jugador con la misma fecha de inicio");
        }

        if (rosterStatus == RosterStatus.ACTIVE
                && teamPlayerRosterRepository.existsByTournamentTeamIdAndPlayerIdAndRosterStatusAndEndDateIsNullAndIdNot(
                current.getTournamentTeamId(),
                current.getPlayerId(),
                RosterStatus.ACTIVE,
                current.getId()
        )) {
            throw new BusinessException("El jugador ya tiene un registro activo abierto en este roster");
        }
    }

    private void validateReferences(Long tournamentTeamId, Long playerId) {
        if (!tournamentTeamRepository.existsById(tournamentTeamId)) {
            throw new BusinessException("El tournamentTeamId enviado no existe");
        }
        if (!playerRepository.existsById(playerId)) {
            throw new BusinessException("El playerId enviado no existe");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new BusinessException("startDate es obligatorio");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("endDate no puede ser menor que startDate");
        }
    }

    private void validateJerseyNumber(Integer jerseyNumber) {
        if (jerseyNumber != null && (jerseyNumber < 0 || jerseyNumber > 99)) {
            throw new BusinessException("jerseyNumber debe estar entre 0 y 99");
        }
    }

    private void validateCaptain(Long tournamentTeamId, Boolean captain, RosterStatus rosterStatus, Long currentId) {
        if (!Boolean.TRUE.equals(captain) || rosterStatus != RosterStatus.ACTIVE) {
            return;
        }

        boolean existsAnotherCaptain = currentId == null
                ? teamPlayerRosterRepository.existsByTournamentTeamIdAndCaptainTrueAndRosterStatus(tournamentTeamId, RosterStatus.ACTIVE)
                : teamPlayerRosterRepository.existsByTournamentTeamIdAndCaptainTrueAndRosterStatusAndIdNot(
                tournamentTeamId, RosterStatus.ACTIVE, currentId);

        if (existsAnotherCaptain) {
            throw new BusinessException("Ya existe un capitan activo para este roster");
        }
    }
}
