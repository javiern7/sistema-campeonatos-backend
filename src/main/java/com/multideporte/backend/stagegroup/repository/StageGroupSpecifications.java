package com.multideporte.backend.stagegroup.repository;

import com.multideporte.backend.stagegroup.entity.StageGroup;
import org.springframework.data.jpa.domain.Specification;

public final class StageGroupSpecifications {

    private StageGroupSpecifications() {
    }

    public static Specification<StageGroup> byFilters(Long stageId, String code) {
        return Specification.where(hasStageId(stageId))
                .and(hasCode(code));
    }

    private static Specification<StageGroup> hasStageId(Long stageId) {
        return (root, query, builder) ->
                stageId == null ? builder.conjunction() : builder.equal(root.get("stageId"), stageId);
    }

    private static Specification<StageGroup> hasCode(String code) {
        return (root, query, builder) -> {
            if (code == null || code.isBlank()) {
                return builder.conjunction();
            }
            return builder.equal(builder.lower(root.get("code")), code.trim().toLowerCase());
        };
    }
}
