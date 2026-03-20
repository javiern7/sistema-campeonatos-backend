package com.multideporte.backend.roster.repository;

import com.multideporte.backend.roster.entity.RosterStatus;
import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import org.springframework.data.jpa.domain.Specification;

public final class TeamPlayerRosterSpecifications {

    private TeamPlayerRosterSpecifications() {
    }

    public static Specification<TeamPlayerRoster> byFilters(
            Long tournamentTeamId,
            Long playerId,
            RosterStatus rosterStatus
    ) {
        return Specification.where(hasTournamentTeamId(tournamentTeamId))
                .and(hasPlayerId(playerId))
                .and(hasRosterStatus(rosterStatus));
    }

    private static Specification<TeamPlayerRoster> hasTournamentTeamId(Long tournamentTeamId) {
        return (root, query, builder) ->
                tournamentTeamId == null ? builder.conjunction() : builder.equal(root.get("tournamentTeamId"), tournamentTeamId);
    }

    private static Specification<TeamPlayerRoster> hasPlayerId(Long playerId) {
        return (root, query, builder) ->
                playerId == null ? builder.conjunction() : builder.equal(root.get("playerId"), playerId);
    }

    private static Specification<TeamPlayerRoster> hasRosterStatus(RosterStatus rosterStatus) {
        return (root, query, builder) ->
                rosterStatus == null ? builder.conjunction() : builder.equal(root.get("rosterStatus"), rosterStatus);
    }
}
