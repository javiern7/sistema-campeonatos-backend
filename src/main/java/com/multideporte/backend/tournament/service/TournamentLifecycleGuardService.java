package com.multideporte.backend.tournament.service;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentLifecycleGuardService {

    private final TournamentTeamRepository tournamentTeamRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final MatchGameRepository matchGameRepository;

    public void assertStructureCanBeModified(Tournament tournament) {
        if (EnumSet.of(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED, TournamentStatus.CANCELLED)
                .contains(tournament.getStatus())) {
            throw new BusinessException("No se permite modificar la estructura de un torneo en progreso, finalizado o cancelado");
        }
    }

    public void assertTournamentDataCanBeUpdated(Tournament tournament, TournamentStatus requestedStatus) {
        if (!tournament.getStatus().equals(requestedStatus)) {
            throw new BusinessException("El status del torneo debe cambiarse usando el endpoint de transicion");
        }

        if (EnumSet.of(TournamentStatus.FINISHED, TournamentStatus.CANCELLED).contains(tournament.getStatus())) {
            throw new BusinessException("No se permite actualizar un torneo finalizado o cancelado");
        }
    }

    public void assertMatchCanBeManaged(Tournament tournament, MatchGameStatus matchStatus) {
        if (EnumSet.of(TournamentStatus.DRAFT, TournamentStatus.CANCELLED, TournamentStatus.FINISHED)
                .contains(tournament.getStatus())) {
            throw new BusinessException("No se permite gestionar partidos para un torneo en estado " + tournament.getStatus());
        }

        if (tournament.getStatus() == TournamentStatus.OPEN && matchStatus != MatchGameStatus.SCHEDULED) {
            throw new BusinessException("Un torneo OPEN solo permite programar partidos en estado SCHEDULED");
        }
    }

    public void assertStatusTransition(Tournament tournament, TournamentStatus targetStatus) {
        if (targetStatus == tournament.getStatus()) {
            throw new BusinessException("El torneo ya se encuentra en el estado solicitado");
        }

        switch (targetStatus) {
            case OPEN -> validateOpenTransition(tournament);
            case IN_PROGRESS -> validateInProgressTransition(tournament);
            case FINISHED -> validateFinishedTransition(tournament);
            case CANCELLED -> validateCancelledTransition(tournament);
            case DRAFT -> throw new BusinessException("No se permite regresar un torneo a DRAFT");
        }
    }

    private void validateOpenTransition(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new BusinessException("Solo un torneo DRAFT puede pasar a OPEN");
        }
    }

    private void validateInProgressTransition(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new BusinessException("Solo un torneo OPEN puede pasar a IN_PROGRESS");
        }

        long approvedTeams = tournamentTeamRepository.countByTournamentIdAndRegistrationStatusIn(
                tournament.getId(),
                List.of(TournamentTeamRegistrationStatus.APPROVED)
        );
        if (approvedTeams < 2) {
            throw new BusinessException("El torneo requiere al menos 2 equipos APPROVED para iniciar");
        }

        List<TournamentStage> stages = tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournament.getId());
        if (stages.isEmpty()) {
            throw new BusinessException("El torneo requiere al menos una etapa configurada para iniciar");
        }

        validateFormatAndStages(tournament.getFormat(), stages);
    }

    private void validateFinishedTransition(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("Solo un torneo IN_PROGRESS puede pasar a FINISHED");
        }

        long completedMatches = matchGameRepository.countByTournamentIdAndStatusIn(
                tournament.getId(),
                List.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT)
        );
        if (completedMatches == 0) {
            throw new BusinessException("No se puede finalizar un torneo sin partidos cerrados");
        }

        if (matchGameRepository.existsByTournamentIdAndStatus(tournament.getId(), MatchGameStatus.SCHEDULED)) {
            throw new BusinessException("No se puede finalizar un torneo con partidos SCHEDULED pendientes");
        }
    }

    private void validateCancelledTransition(Tournament tournament) {
        if (!EnumSet.of(TournamentStatus.DRAFT, TournamentStatus.OPEN).contains(tournament.getStatus())) {
            throw new BusinessException("Solo un torneo DRAFT u OPEN puede pasar a CANCELLED");
        }
    }

    private void validateFormatAndStages(TournamentFormat format, List<TournamentStage> stages) {
        boolean hasGroupStage = stages.stream().anyMatch(stage -> stage.getStageType() == TournamentStageType.GROUP_STAGE);
        boolean hasKnockoutStage = stages.stream().anyMatch(stage -> stage.getStageType() == TournamentStageType.KNOCKOUT);

        switch (format) {
            case LEAGUE -> {
                if (hasKnockoutStage) {
                    throw new BusinessException("Un torneo LEAGUE no debe tener etapas KNOCKOUT para iniciar");
                }
            }
            case KNOCKOUT -> {
                if (!hasKnockoutStage) {
                    throw new BusinessException("Un torneo KNOCKOUT requiere al menos una etapa KNOCKOUT para iniciar");
                }
                if (hasGroupStage) {
                    throw new BusinessException("Un torneo KNOCKOUT no debe tener etapas GROUP_STAGE para iniciar");
                }
            }
            case GROUPS_THEN_KNOCKOUT -> {
                if (!hasGroupStage || !hasKnockoutStage) {
                    throw new BusinessException("Un torneo GROUPS_THEN_KNOCKOUT requiere fases GROUP_STAGE y KNOCKOUT para iniciar");
                }
            }
        }
    }
}
