package com.multideporte.backend.tournament.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.sport.repository.SportRepository;
import com.multideporte.backend.tournament.dto.request.TournamentCreateRequest;
import com.multideporte.backend.tournament.dto.request.TournamentUpdateRequest;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TournamentValidator {

    private final SportRepository sportRepository;
    private final TournamentRepository tournamentRepository;

    public void validateForCreate(TournamentCreateRequest request) {
        validateCommon(
                request.sportId(),
                request.startDate(),
                request.endDate(),
                request.registrationOpenAt(),
                request.registrationCloseAt(),
                request.maxTeams(),
                request.pointsWin(),
                request.pointsDraw(),
                request.pointsLoss()
        );

        String slug = buildSlug(request.name(), request.seasonName());
        if (tournamentRepository.existsBySlug(slug)) {
            throw new BusinessException("Ya existe un torneo con el slug generado: " + slug);
        }
    }

    public void validateForUpdate(Tournament current, TournamentUpdateRequest request) {
        validateCommon(
                request.sportId(),
                request.startDate(),
                request.endDate(),
                request.registrationOpenAt(),
                request.registrationCloseAt(),
                request.maxTeams(),
                request.pointsWin(),
                request.pointsDraw(),
                request.pointsLoss()
        );

        String slug = buildSlug(request.name(), request.seasonName());
        tournamentRepository.findBySlug(slug)
                .filter(found -> !found.getId().equals(current.getId()))
                .ifPresent(found -> {
                    throw new BusinessException("Ya existe otro torneo con el slug generado: " + slug);
                });
    }

    public String buildSlug(String name, String seasonName) {
        String base = (name + "-" + seasonName).trim().toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (slug.isBlank()) {
            throw new BusinessException("No se pudo generar un slug valido para el torneo");
        }

        return slug;
    }

    private void validateCommon(
            Long sportId,
            LocalDate startDate,
            LocalDate endDate,
            OffsetDateTime registrationOpenAt,
            OffsetDateTime registrationCloseAt,
            Integer maxTeams,
            Integer pointsWin,
            Integer pointsDraw,
            Integer pointsLoss
    ) {
        if (!sportRepository.existsById(sportId)) {
            throw new BusinessException("El sportId enviado no existe");
        }

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("endDate no puede ser menor que startDate");
        }

        if (registrationOpenAt != null && registrationCloseAt != null && registrationCloseAt.isBefore(registrationOpenAt)) {
            throw new BusinessException("registrationCloseAt no puede ser menor que registrationOpenAt");
        }

        if (maxTeams != null && maxTeams < 2) {
            throw new BusinessException("maxTeams debe ser mayor o igual a 2");
        }

        if (pointsWin < pointsDraw || pointsDraw < pointsLoss || pointsLoss < 0) {
            throw new BusinessException("La configuracion de puntos es invalida");
        }
    }
}
