package com.multideporte.backend.standing.repository;

import com.multideporte.backend.standing.entity.Standing;
import org.springframework.data.jpa.domain.Specification;

public final class StandingSpecifications {

    private StandingSpecifications() {
    }

    public static Specification<Standing> byFilters(Long tournamentId, Long stageId, Long groupId, Long tournamentTeamId) {
        return Specification.where(hasTournamentId(tournamentId))
                .and(hasStageId(stageId))
                .and(hasGroupId(groupId))
                .and(hasTournamentTeamId(tournamentTeamId));
    }

    private static Specification<Standing> hasTournamentId(Long tournamentId) {
        return (root, query, builder) ->
                tournamentId == null ? builder.conjunction() : builder.equal(root.get("tournamentId"), tournamentId);
    }

    private static Specification<Standing> hasStageId(Long stageId) {
        return (root, query, builder) ->
                stageId == null ? builder.conjunction() : builder.equal(root.get("stageId"), stageId);
    }

    private static Specification<Standing> hasGroupId(Long groupId) {
        return (root, query, builder) ->
                groupId == null ? builder.conjunction() : builder.equal(root.get("groupId"), groupId);
    }

    private static Specification<Standing> hasTournamentTeamId(Long tournamentTeamId) {
        return (root, query, builder) ->
                tournamentTeamId == null ? builder.conjunction() : builder.equal(root.get("tournamentTeamId"), tournamentTeamId);
    }
}
