package com.multideporte.backend.team.repository;

import com.multideporte.backend.team.entity.Team;
import org.springframework.data.jpa.domain.Specification;

public final class TeamSpecifications {

    private TeamSpecifications() {
    }

    public static Specification<Team> byFilters(String name, String code, Boolean active) {
        return Specification.where(hasName(name))
                .and(hasCode(code))
                .and(hasActive(active));
    }

    private static Specification<Team> hasName(String name) {
        return (root, query, builder) -> {
            if (name == null || name.isBlank()) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Team> hasCode(String code) {
        return (root, query, builder) -> {
            if (code == null || code.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(builder.lower(root.get("code")), code.trim().toLowerCase());
        };
    }

    private static Specification<Team> hasActive(Boolean active) {
        return (root, query, builder) ->
                active == null ? builder.conjunction() : builder.equal(root.get("active"), active);
    }
}
