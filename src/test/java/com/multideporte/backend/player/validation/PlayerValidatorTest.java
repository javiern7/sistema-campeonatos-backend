package com.multideporte.backend.player.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerValidatorTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerValidator playerValidator;

    @Test
    void shouldNormalizeDocumentFields() {
        assertEquals("DNI", playerValidator.normalizeDocumentType(" dni "));
        assertEquals("12345678A", playerValidator.normalizeDocumentNumber(" 12345678a "));
    }

    @Test
    void shouldFailWhenDocumentPairIsIncomplete() {
        assertThrows(BusinessException.class, () ->
                playerValidator.validateForCreate("DNI", null, LocalDate.of(2000, 1, 1), "+51999999999"));
    }

    @Test
    void shouldFailWhenDocumentAlreadyExists() {
        Player existing = new Player();
        existing.setId(1L);

        when(playerRepository.findByDocumentTypeIgnoreCaseAndDocumentNumberIgnoreCase("DNI", "12345678"))
                .thenReturn(Optional.of(existing));

        assertThrows(BusinessException.class, () ->
                playerValidator.validateForCreate("dni", "12345678", LocalDate.of(2000, 1, 1), "+51999999999"));
    }

    @Test
    void shouldFailWhenPhoneFormatIsInvalid() {
        assertThrows(BusinessException.class, () ->
                playerValidator.validateForCreate(null, null, LocalDate.of(2000, 1, 1), "abc"));
    }
}
