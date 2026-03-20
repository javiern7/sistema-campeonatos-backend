package com.multideporte.backend.tournamentteam.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamSpecifications;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentTeamValidator {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    public void validateForCreate(
            Long tournamentId,
            Long teamId,
            TournamentTeamRegistrationStatus registrationStatus,
            Integer seedNumber,
            Integer groupDrawPosition
    ) {
        Tournament tournament = loadTournament(tournamentId);
        validateTeamExists(teamId);
        validateTournamentState(tournament);
        validateNumbers(seedNumber, groupDrawPosition);

        if (tournamentTeamRepository.existsByTournamentIdAndTeamId(tournamentId, teamId)) {
            throw new BusinessException("El equipo ya esta registrado en el torneo");
        }

        validateSeedUniqueness(null, tournamentId, seedNumber);
        validateTournamentCapacity(tournament, registrationStatus);
    }

    public void validateForUpdate(
            TournamentTeam current,
            TournamentTeamRegistrationStatus registrationStatus,
            Integer seedNumber,
            Integer groupDrawPosition
    ) {
        Tournament tournament = loadTournament(current.getTournamentId());
        validateTournamentState(tournament);
        validateNumbers(seedNumber, groupDrawPosition);
        validateSeedUniqueness(current.getId(), current.getTournamentId(), seedNumber);
        validateTournamentCapacityForUpdate(tournament, current, registrationStatus);
    }

    private Tournament loadTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException("El tournamentId enviado no existe"));
    }

    private void validateTeamExists(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new BusinessException("El teamId enviado no existe");
        }
    }

    private void validateTournamentState(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.CANCELLED || tournament.getStatus() == TournamentStatus.FINISHED) {
            throw new BusinessException("No se permite modificar inscripciones en un torneo cancelado o finalizado");
        }
    }

    private void validateNumbers(Integer seedNumber, Integer groupDrawPosition) {
        if (seedNumber != null && seedNumber < 1) {
            throw new BusinessException("seedNumber debe ser mayor a 0");
        }
        if (groupDrawPosition != null && groupDrawPosition < 1) {
            throw new BusinessException("groupDrawPosition debe ser mayor a 0");
        }
    }

    private void validateSeedUniqueness(Long currentId, Long tournamentId, Integer seedNumber) {
        if (seedNumber == null) {
            return;
        }

        tournamentTeamRepository.findAll(TournamentTeamSpecifications.byFilters(tournamentId, null, null))
                .stream()
                .filter(item -> seedNumber.equals(item.getSeedNumber()))
                .filter(item -> currentId == null || !item.getId().equals(currentId))
                .findFirst()
                .ifPresent(item -> {
                    throw new BusinessException("El seedNumber ya esta asignado en este torneo");
                });
    }

    private void validateTournamentCapacity(Tournament tournament, TournamentTeamRegistrationStatus newStatus) {
        if (tournament.getMaxTeams() == null) {
            return;
        }

        if (!List.of(TournamentTeamRegistrationStatus.PENDING, TournamentTeamRegistrationStatus.APPROVED).contains(newStatus)) {
            return;
        }

        long registeredCount = tournamentTeamRepository.countByTournamentIdAndRegistrationStatusIn(
                tournament.getId(),
                List.of(TournamentTeamRegistrationStatus.PENDING, TournamentTeamRegistrationStatus.APPROVED)
        );

        if (registeredCount >= tournament.getMaxTeams()) {
            throw new BusinessException("El torneo ya alcanzo el maximo de equipos permitidos");
        }
    }

    private void validateTournamentCapacityForUpdate(
            Tournament tournament,
            TournamentTeam current,
            TournamentTeamRegistrationStatus newStatus
    ) {
        if (tournament.getMaxTeams() == null) {
            return;
        }

        boolean currentConsumesSlot = List.of(
                TournamentTeamRegistrationStatus.PENDING,
                TournamentTeamRegistrationStatus.APPROVED
        ).contains(current.getRegistrationStatus());

        boolean newConsumesSlot = List.of(
                TournamentTeamRegistrationStatus.PENDING,
                TournamentTeamRegistrationStatus.APPROVED
        ).contains(newStatus);

        if (!newConsumesSlot || currentConsumesSlot) {
            return;
        }

        long registeredCount = tournamentTeamRepository.countByTournamentIdAndRegistrationStatusIn(
                tournament.getId(),
                List.of(TournamentTeamRegistrationStatus.PENDING, TournamentTeamRegistrationStatus.APPROVED)
        );

        if (registeredCount >= tournament.getMaxTeams()) {
            throw new BusinessException("El torneo ya alcanzo el maximo de equipos permitidos");
        }
    }
}
