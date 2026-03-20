package com.multideporte.backend.stagegroup.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StageGroupValidator {

    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;

    public void validateForCreate(Long stageId, String code, Integer sequenceOrder) {
        TournamentStage stage = loadStage(stageId);
        validateStageAllowsGroups(stage);
        validateCodeAndSequence(stageId, code, sequenceOrder, null);
    }

    public void validateForUpdate(StageGroup current, String code, Integer sequenceOrder) {
        TournamentStage stage = loadStage(current.getStageId());
        validateStageAllowsGroups(stage);
        validateCodeAndSequence(current.getStageId(), code, sequenceOrder, current.getId());
    }

    public String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private TournamentStage loadStage(Long stageId) {
        return tournamentStageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));
    }

    private void validateStageAllowsGroups(TournamentStage stage) {
        if (stage.getStageType() != TournamentStageType.GROUP_STAGE) {
            throw new BusinessException("Solo los stages de tipo GROUP_STAGE pueden tener grupos");
        }
    }

    private void validateCodeAndSequence(Long stageId, String code, Integer sequenceOrder, Long currentId) {
        String normalizedCode = normalizeCode(code);

        boolean codeExists = currentId == null
                ? stageGroupRepository.existsByStageIdAndCodeIgnoreCase(stageId, normalizedCode)
                : stageGroupRepository.existsByStageIdAndCodeIgnoreCaseAndIdNot(stageId, normalizedCode, currentId);

        if (codeExists) {
            throw new BusinessException("Ya existe un grupo con ese code dentro del stage");
        }

        boolean sequenceExists = currentId == null
                ? stageGroupRepository.existsByStageIdAndSequenceOrder(stageId, sequenceOrder)
                : stageGroupRepository.existsByStageIdAndSequenceOrderAndIdNot(stageId, sequenceOrder, currentId);

        if (sequenceExists) {
            throw new BusinessException("Ya existe un grupo con ese sequenceOrder dentro del stage");
        }
    }
}
