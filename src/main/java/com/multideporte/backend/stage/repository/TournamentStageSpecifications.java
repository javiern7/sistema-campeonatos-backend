package com.multideporte.backend.stage.repository;

import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import org.springframework.data.jpa.domain.Specification;

public final class TournamentStageSpecifications {

    private TournamentStageSpecifications() {
    }

    public static Specification<TournamentStage> byFilters(Long tournamentId, TournamentStageType stageType, Boolean active) {
        return Specification.where(hasTournamentId(tournamentId))
                .and(hasStageType(stageType))
                .and(hasActive(active));
    }

    private static Specification<TournamentStage> hasTournamentId(Long tournamentId) {
        return (root, query, builder) ->
                tournamentId == null ? builder.conjunction() : builder.equal(root.get("tournamentId"), tournamentId);
    }

    private static Specification<TournamentStage> hasStageType(TournamentStageType stageType) {
        return (root, query, builder) ->
                stageType == null ? builder.conjunction() : builder.equal(root.get("stageType"), stageType);
    }

    private static Specification<TournamentStage> hasActive(Boolean active) {
        return (root, query, builder) ->
                active == null ? builder.conjunction() : builder.equal(root.get("active"), active);
    }
}
