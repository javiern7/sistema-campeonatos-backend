package com.multideporte.backend.match.repository;

import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import org.springframework.data.jpa.domain.Specification;

public final class MatchGameSpecifications {

    private MatchGameSpecifications() {
    }

    public static Specification<MatchGame> byFilters(
            Long tournamentId,
            Long stageId,
            Long groupId,
            MatchGameStatus status
    ) {
        return Specification.where(hasTournamentId(tournamentId))
                .and(hasStageId(stageId))
                .and(hasGroupId(groupId))
                .and(hasStatus(status));
    }

    private static Specification<MatchGame> hasTournamentId(Long tournamentId) {
        return (root, query, builder) ->
                tournamentId == null ? builder.conjunction() : builder.equal(root.get("tournamentId"), tournamentId);
    }

    private static Specification<MatchGame> hasStageId(Long stageId) {
        return (root, query, builder) ->
                stageId == null ? builder.conjunction() : builder.equal(root.get("stageId"), stageId);
    }

    private static Specification<MatchGame> hasGroupId(Long groupId) {
        return (root, query, builder) ->
                groupId == null ? builder.conjunction() : builder.equal(root.get("groupId"), groupId);
    }

    private static Specification<MatchGame> hasStatus(MatchGameStatus status) {
        return (root, query, builder) ->
                status == null ? builder.conjunction() : builder.equal(root.get("status"), status);
    }
}
