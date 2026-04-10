package com.multideporte.backend.security.usermanagement;

import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppUser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class AppUserSpecifications {

    private AppUserSpecifications() {
    }

    public static Specification<AppUser> byFilters(String query, String status, String roleCode) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);
            Join<AppUser, AppRole> roleJoin = root.join("roles", JoinType.LEFT);

            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (query != null && !query.isBlank()) {
                String normalizedQuery = "%" + query.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), normalizedQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), normalizedQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), normalizedQuery),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), normalizedQuery)
                ));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.trim()));
            }

            if (roleCode != null && !roleCode.isBlank()) {
                predicates.add(criteriaBuilder.equal(roleJoin.get("code"), roleCode.trim()));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
