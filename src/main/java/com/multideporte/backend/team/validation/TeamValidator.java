package com.multideporte.backend.team.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamValidator {

    private final TeamRepository teamRepository;

    public void validateForCreate(String code, String primaryColor, String secondaryColor) {
        validateCodeUniqueness(null, code);
        validateColors(primaryColor, secondaryColor);
    }

    public void validateForUpdate(Team team, String code, String primaryColor, String secondaryColor) {
        validateCodeUniqueness(team.getId(), code);
        validateColors(primaryColor, secondaryColor);
    }

    public String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void validateCodeUniqueness(Long currentId, String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return;
        }

        teamRepository.findByCodeIgnoreCase(normalizedCode)
                .filter(team -> currentId == null || !team.getId().equals(currentId))
                .ifPresent(team -> {
                    throw new BusinessException("Ya existe un equipo con el code enviado");
                });
    }

    private void validateColors(String primaryColor, String secondaryColor) {
        validateColor(primaryColor, "primaryColor");
        validateColor(secondaryColor, "secondaryColor");
    }

    private void validateColor(String color, String fieldName) {
        if (color == null || color.isBlank()) {
            return;
        }

        String normalized = color.trim();
        if (!normalized.matches("^#?[A-Fa-f0-9]{3,8}$")) {
            throw new BusinessException(fieldName + " debe tener formato hexadecimal valido");
        }
    }
}
