package com.multideporte.backend.stage.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentStageValidator {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRepository tournamentStageRepository;

    public void validateForCreate(
            Long tournamentId,
            TournamentStageType stageType,
            Integer sequenceOrder,
            Integer legs,
            Boolean roundTrip
    ) {
        validateTournamentExists(tournamentId);
        validateStageRules(stageType, legs, roundTrip);

        if (tournamentStageRepository.existsByTournamentIdAndSequenceOrder(tournamentId, sequenceOrder)) {
            throw new BusinessException("Ya existe una etapa con ese sequenceOrder en el torneo");
        }
    }

    public void validateForUpdate(
            TournamentStage current,
            TournamentStageType stageType,
            Integer sequenceOrder,
            Integer legs,
            Boolean roundTrip
    ) {
        validateTournamentExists(current.getTournamentId());
        validateStageRules(stageType, legs, roundTrip);

        if (tournamentStageRepository.existsByTournamentIdAndSequenceOrderAndIdNot(
                current.getTournamentId(),
                sequenceOrder,
                current.getId()
        )) {
            throw new BusinessException("Ya existe otra etapa con ese sequenceOrder en el torneo");
        }
    }

    private void validateTournamentExists(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new BusinessException("El tournamentId enviado no existe");
        }
    }

    private void validateStageRules(TournamentStageType stageType, Integer legs, Boolean roundTrip) {
        if (legs == null || legs < 1) {
            throw new BusinessException("legs debe ser mayor a 0");
        }

        if (stageType == TournamentStageType.KNOCKOUT && Boolean.TRUE.equals(roundTrip) && legs < 2) {
            throw new BusinessException("Un stage KNOCKOUT con roundTrip requiere al menos 2 legs");
        }

        if (stageType == TournamentStageType.LEAGUE && legs > 2) {
            throw new BusinessException("Para MVP, un stage LEAGUE no debe exceder 2 legs");
        }
    }
}
