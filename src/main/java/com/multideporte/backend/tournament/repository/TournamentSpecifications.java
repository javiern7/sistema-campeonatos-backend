package com.multideporte.backend.tournament.repository;

import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TournamentSpecifications {

    private TournamentSpecifications() {
    }

    public static Specification<Tournament> byFilters(String name, Long sportId, TournamentStatus status) {
        return Specification
                .where(hasName(name))
                .and(hasSportId(sportId))
                .and(hasStatus(status));
    }

    private static Specification<Tournament> hasName(String name) {
        return (root, query, builder) -> {
            if (name == null || name.isBlank()) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Tournament> hasSportId(Long sportId) {
        return (root, query, builder) ->
                sportId == null ? builder.conjunction() : builder.equal(root.get("sportId"), sportId);
    }

    private static Specification<Tournament> hasStatus(TournamentStatus status) {
        return (root, query, builder) ->
                status == null ? builder.conjunction() : builder.equal(root.get("status"), status);
    }
}
