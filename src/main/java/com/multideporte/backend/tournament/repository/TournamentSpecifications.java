package com.multideporte.backend.tournament.repository;

import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentOperationalCategory;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TournamentSpecifications {

    private TournamentSpecifications() {
    }

    public static Specification<Tournament> byFilters(
            String name,
            Long sportId,
            TournamentStatus status,
            TournamentOperationalCategory operationalCategory,
            Boolean executiveOnly
    ) {
        return Specification
                .where(hasName(name))
                .and(hasSportId(sportId))
                .and(hasStatus(status))
                .and(hasOperationalCategory(operationalCategory))
                .and(hasExecutiveVisibility(executiveOnly));
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

    private static Specification<Tournament> hasOperationalCategory(TournamentOperationalCategory operationalCategory) {
        return (root, query, builder) -> operationalCategory == null
                ? builder.conjunction()
                : builder.equal(root.get("operationalCategory"), operationalCategory);
    }

    private static Specification<Tournament> hasExecutiveVisibility(Boolean executiveOnly) {
        return (root, query, builder) -> Boolean.TRUE.equals(executiveOnly)
                ? builder.equal(root.get("operationalCategory"), TournamentOperationalCategory.PRODUCTION)
                : builder.conjunction();
    }
}
