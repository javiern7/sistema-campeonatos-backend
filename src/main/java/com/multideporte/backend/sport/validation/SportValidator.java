package com.multideporte.backend.sport.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.entity.SportPosition;
import com.multideporte.backend.sport.repository.SportPositionRepository;
import com.multideporte.backend.sport.repository.SportRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SportValidator {

    private final SportRepository sportRepository;
    private final SportPositionRepository sportPositionRepository;

    public void validateSportForCreate(String code, String scoreLabel) {
        validateCodeFormat(code, "code");
        validateSportCodeUniqueness(null, code);
        validateScoreLabel(scoreLabel);
    }

    public void validateSportForUpdate(Sport sport, String code, String scoreLabel) {
        validateCodeFormat(code, "code");
        validateSportCodeUniqueness(sport.getId(), code);
        validateScoreLabel(scoreLabel);
    }

    public void validatePositionForCreate(Long sportId, String code, Integer displayOrder) {
        validateCodeFormat(code, "code");
        validatePositionCodeUniqueness(sportId, null, code);
        validatePositionDisplayOrderUniqueness(sportId, null, displayOrder);
    }

    public void validatePositionForUpdate(SportPosition position, String code, Integer displayOrder) {
        Long sportId = position.getSport().getId();
        validateCodeFormat(code, "code");
        validatePositionCodeUniqueness(sportId, position.getId(), code);
        validatePositionDisplayOrderUniqueness(sportId, position.getId(), displayOrder);
    }

    public String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private void validateSportCodeUniqueness(Long currentId, String code) {
        String normalizedCode = normalizeCode(code);
        sportRepository.findByCodeIgnoreCase(normalizedCode)
                .filter(sport -> currentId == null || !sport.getId().equals(currentId))
                .ifPresent(sport -> {
                    throw new BusinessException("Ya existe un deporte con el code enviado");
                });
    }

    private void validatePositionCodeUniqueness(Long sportId, Long currentId, String code) {
        String normalizedCode = normalizeCode(code);
        sportPositionRepository.findBySportIdAndCodeIgnoreCase(sportId, normalizedCode)
                .filter(position -> currentId == null || !position.getId().equals(currentId))
                .ifPresent(position -> {
                    throw new BusinessException("Ya existe una posicion con el code enviado para este deporte");
                });
    }

    private void validatePositionDisplayOrderUniqueness(Long sportId, Long currentId, Integer displayOrder) {
        sportPositionRepository.findBySportIdAndDisplayOrder(sportId, displayOrder)
                .filter(position -> currentId == null || !position.getId().equals(currentId))
                .ifPresent(position -> {
                    throw new BusinessException("Ya existe una posicion con el displayOrder enviado para este deporte");
                });
    }

    private void validateCodeFormat(String code, String fieldName) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null || !normalizedCode.matches("^[A-Z0-9_]+$")) {
            throw new BusinessException(fieldName + " solo permite letras, numeros y guion bajo");
        }
    }

    private void validateScoreLabel(String scoreLabel) {
        String normalized = normalizeText(scoreLabel);
        if (normalized != null && !normalized.matches("^[A-Za-z0-9_ -]+$")) {
            throw new BusinessException("scoreLabel contiene caracteres no soportados");
        }
    }
}
