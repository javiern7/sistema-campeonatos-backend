package com.multideporte.backend.player.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.player.entity.Player;
import com.multideporte.backend.player.repository.PlayerRepository;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayerValidator {

    private final PlayerRepository playerRepository;

    public void validateForCreate(
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phone
    ) {
        validateDocumentUniqueness(null, documentType, documentNumber);
        validateBirthDate(birthDate);
        validatePhone(phone);
    }

    public void validateForUpdate(
            Player current,
            String documentType,
            String documentNumber,
            LocalDate birthDate,
            String phone
    ) {
        validateDocumentUniqueness(current.getId(), documentType, documentNumber);
        validateBirthDate(birthDate);
        validatePhone(phone);
    }

    public String normalizeDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return null;
        }
        return documentType.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizeDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            return null;
        }
        return documentNumber.trim().toUpperCase(Locale.ROOT);
    }

    private void validateDocumentUniqueness(Long currentId, String documentType, String documentNumber) {
        String normalizedType = normalizeDocumentType(documentType);
        String normalizedNumber = normalizeDocumentNumber(documentNumber);

        if ((normalizedType == null) != (normalizedNumber == null)) {
            throw new BusinessException("documentType y documentNumber deben enviarse juntos");
        }

        if (normalizedType == null) {
            return;
        }

        playerRepository.findByDocumentTypeIgnoreCaseAndDocumentNumberIgnoreCase(normalizedType, normalizedNumber)
                .filter(player -> currentId == null || !player.getId().equals(currentId))
                .ifPresent(player -> {
                    throw new BusinessException("Ya existe un jugador con el documento enviado");
                });
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException("birthDate no puede estar en el futuro");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }

        String normalized = phone.trim();
        if (!normalized.matches("^[0-9+\\-()\\s]{6,30}$")) {
            throw new BusinessException("phone tiene formato invalido");
        }
    }
}
