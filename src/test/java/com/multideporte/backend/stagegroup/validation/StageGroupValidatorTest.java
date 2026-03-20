package com.multideporte.backend.stagegroup.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StageGroupValidatorTest {

    @Mock
    private TournamentStageRepository tournamentStageRepository;

    @Mock
    private StageGroupRepository stageGroupRepository;

    @InjectMocks
    private StageGroupValidator stageGroupValidator;

    @Test
    void shouldNormalizeCodeToUpperCase() {
        assertEquals("GRUPO-A", stageGroupValidator.normalizeCode(" grupo-a "));
    }

    @Test
    void shouldFailWhenStageIsNotGroupStage() {
        TournamentStage stage = new TournamentStage();
        stage.setId(1L);
        stage.setStageType(TournamentStageType.LEAGUE);

        when(tournamentStageRepository.findById(1L)).thenReturn(Optional.of(stage));

        assertThrows(BusinessException.class, () ->
                stageGroupValidator.validateForCreate(1L, "A", 1));
    }

    @Test
    void shouldFailWhenCodeAlreadyExists() {
        TournamentStage stage = new TournamentStage();
        stage.setId(1L);
        stage.setStageType(TournamentStageType.GROUP_STAGE);

        when(tournamentStageRepository.findById(1L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.existsByStageIdAndCodeIgnoreCase(1L, "A")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                stageGroupValidator.validateForCreate(1L, "a", 1));
    }
}
