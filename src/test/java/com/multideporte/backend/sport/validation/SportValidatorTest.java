package com.multideporte.backend.sport.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.entity.SportPosition;
import com.multideporte.backend.sport.repository.SportPositionRepository;
import com.multideporte.backend.sport.repository.SportRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SportValidatorTest {

    @Mock
    private SportRepository sportRepository;

    @Mock
    private SportPositionRepository sportPositionRepository;

    private SportValidator sportValidator;

    @BeforeEach
    void setUp() {
        sportValidator = new SportValidator(sportRepository, sportPositionRepository);
    }

    @Test
    void shouldRejectDuplicatedSportCode() {
        Sport existing = new Sport();
        existing.setId(10L);
        when(sportRepository.findByCodeIgnoreCase("FOOTBALL")).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sportValidator.validateSportForCreate(" football ", "GOALS")
        );

        assertEquals("Ya existe un deporte con el code enviado", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidCodeFormat() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sportValidator.validateSportForCreate("bad code", "POINTS")
        );

        assertEquals("code solo permite letras, numeros y guion bajo", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicatedPositionDisplayOrderWithinSport() {
        Sport sport = new Sport();
        sport.setId(3L);

        SportPosition existing = new SportPosition();
        existing.setId(8L);
        existing.setSport(sport);
        when(sportPositionRepository.findBySportIdAndCodeIgnoreCase(3L, "GK")).thenReturn(Optional.empty());
        when(sportPositionRepository.findBySportIdAndDisplayOrder(3L, 1)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> sportValidator.validatePositionForCreate(3L, "gk", 1)
        );

        assertEquals("Ya existe una posicion con el displayOrder enviado para este deporte", exception.getMessage());
    }

    @Test
    void shouldNormalizeCodesAndTrimText() {
        assertEquals("FUTSAL", sportValidator.normalizeCode(" futsal "));
        assertEquals("Goles", sportValidator.normalizeText(" Goles "));
    }
}
