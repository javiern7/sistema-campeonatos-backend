package com.multideporte.backend.standing.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.repository.TeamPlayerRosterRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StandingValidator {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TeamPlayerRosterRepository teamPlayerRosterRepository;
    private final StandingRepository standingRepository;

    public void validateForCreate(Standing standing) {
        validateReferences(standing);
        validateMetrics(standing);
        validateUniqueScope(null, standing);
    }

    public void validateForUpdate(Standing current, Standing candidate) {
        validateReferences(candidate);
        validateMetrics(candidate);
        validateUniqueScope(current.getId(), candidate);
    }

    private void validateReferences(Standing standing) {
        if (!tournamentRepository.existsById(standing.getTournamentId())) {
            throw new BusinessException("El tournamentId enviado no existe");
        }

        if (standing.getStageId() != null) {
            TournamentStage stage = tournamentStageRepository.findById(standing.getStageId())
                    .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));

            if (!stage.getTournamentId().equals(standing.getTournamentId())) {
                throw new BusinessException("El stageId no pertenece al torneo indicado");
            }
        }

        if (standing.getGroupId() != null) {
            if (standing.getStageId() == null) {
                throw new BusinessException("groupId requiere un stageId");
            }
            StageGroup group = stageGroupRepository.findById(standing.getGroupId())
                    .orElseThrow(() -> new BusinessException("El groupId enviado no existe"));

            if (!group.getStageId().equals(standing.getStageId())) {
                throw new BusinessException("El groupId no pertenece al stageId indicado");
            }
        }

        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(standing.getTournamentTeamId())
                .orElseThrow(() -> new BusinessException("El tournamentTeamId enviado no existe"));

        if (!tournamentTeam.getTournamentId().equals(standing.getTournamentId())) {
            throw new BusinessException("El tournamentTeamId no pertenece al torneo indicado");
        }

        if (tournamentTeam.getRegistrationStatus() != TournamentTeamRegistrationStatus.APPROVED) {
            throw new BusinessException("Un standing requiere una inscripcion APPROVED");
        }

        if (standing.getPlayed() > 0
                && !teamPlayerRosterRepository.existsByTournamentTeamIdAndRosterStatusAndEndDateIsNull(
                standing.getTournamentTeamId(),
                RosterStatus.ACTIVE
        )) {
            throw new BusinessException("Un standing con actividad requiere roster ACTIVE para la inscripcion");
        }
    }

    private void validateMetrics(Standing standing) {
        if (standing.getPlayed() < 0 || standing.getWins() < 0 || standing.getDraws() < 0 || standing.getLosses() < 0
                || standing.getPointsFor() < 0 || standing.getPointsAgainst() < 0 || standing.getPoints() < 0) {
            throw new BusinessException("Los valores de standing no pueden ser negativos");
        }

        if (standing.getWins() + standing.getDraws() + standing.getLosses() != standing.getPlayed()) {
            throw new BusinessException("wins + draws + losses debe ser igual a played");
        }

        if (standing.getScoreDiff() != standing.getPointsFor() - standing.getPointsAgainst()) {
            throw new BusinessException("scoreDiff debe ser igual a pointsFor - pointsAgainst");
        }

        if (standing.getRankPosition() != null && standing.getRankPosition() < 1) {
            throw new BusinessException("rankPosition debe ser mayor a 0");
        }
    }

    private void validateUniqueScope(Long currentId, Standing standing) {
        standingRepository.findByTournamentIdAndStageIdAndGroupIdAndTournamentTeamId(
                        standing.getTournamentId(),
                        standing.getStageId(),
                        standing.getGroupId(),
                        standing.getTournamentTeamId()
                )
                .filter(found -> currentId == null || !found.getId().equals(currentId))
                .ifPresent(found -> {
                    throw new BusinessException("Ya existe un standing para ese alcance y tournamentTeam");
                });
    }
}
