package com.multideporte.backend.player.repository;

import com.multideporte.backend.player.entity.Player;
import org.springframework.data.jpa.domain.Specification;

public final class PlayerSpecifications {

    private PlayerSpecifications() {
    }

    public static Specification<Player> byFilters(
            String search,
            String documentType,
            String documentNumber,
            Boolean active
    ) {
        return Specification.where(hasSearch(search))
                .and(hasDocumentType(documentType))
                .and(hasDocumentNumber(documentNumber))
                .and(hasActive(active));
    }

    private static Specification<Player> hasSearch(String search) {
        return (root, query, builder) -> {
            if (search == null || search.isBlank()) {
                return builder.conjunction();
            }
            String value = "%" + search.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("firstName")), value),
                    builder.like(builder.lower(root.get("lastName")), value)
            );
        };
    }

    private static Specification<Player> hasDocumentType(String documentType) {
        return (root, query, builder) -> {
            if (documentType == null || documentType.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(builder.lower(root.get("documentType")), documentType.trim().toLowerCase());
        };
    }

    private static Specification<Player> hasDocumentNumber(String documentNumber) {
        return (root, query, builder) -> {
            if (documentNumber == null || documentNumber.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(builder.lower(root.get("documentNumber")), documentNumber.trim().toLowerCase());
        };
    }

    private static Specification<Player> hasActive(Boolean active) {
        return (root, query, builder) ->
                active == null ? builder.conjunction() : builder.equal(root.get("active"), active);
    }
}
