package com.multideporte.backend.security.audit;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class OperationalAuditSpecifications {

    private OperationalAuditSpecifications() {
    }

    public static Specification<OperationalAuditEvent> byFilters(
            String action,
            String entityType,
            String actor,
            OperationalAuditResult result,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return Specification.where(hasAction(action))
                .and(hasEntityType(entityType))
                .and(hasActor(actor))
                .and(hasResult(result))
                .and(occurredFrom(from))
                .and(occurredTo(to));
    }

    private static Specification<OperationalAuditEvent> hasAction(String action) {
        return (root, query, builder) -> {
            if (action == null || action.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(root.get("action"), action.trim());
        };
    }

    private static Specification<OperationalAuditEvent> hasEntityType(String entityType) {
        return (root, query, builder) -> {
            if (entityType == null || entityType.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(root.get("entityType"), entityType.trim());
        };
    }

    private static Specification<OperationalAuditEvent> hasActor(String actor) {
        return (root, query, builder) -> {
            if (actor == null || actor.isBlank()) {
                return builder.conjunction();
            }
            return builder.like(builder.lower(root.get("actorUsername")), "%" + actor.trim().toLowerCase() + "%");
        };
    }

    private static Specification<OperationalAuditEvent> hasResult(OperationalAuditResult result) {
        return (root, query, builder) ->
                result == null ? builder.conjunction() : builder.equal(root.get("result"), result);
    }

    private static Specification<OperationalAuditEvent> occurredFrom(OffsetDateTime from) {
        return (root, query, builder) ->
                from == null ? builder.conjunction() : builder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<OperationalAuditEvent> occurredTo(OffsetDateTime to) {
        return (root, query, builder) ->
                to == null ? builder.conjunction() : builder.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}
