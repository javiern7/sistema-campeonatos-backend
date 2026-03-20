package com.multideporte.backend.tournamentteam.repository;

import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TournamentTeamSpecifications {

    private TournamentTeamSpecifications() {
    }

    public static Specification<TournamentTeam> byFilters(
            Long tournamentId,
            Long teamId,
            TournamentTeamRegistrationStatus registrationStatus
    ) {
        return Specification.where(hasTournamentId(tournamentId))
                .and(hasTeamId(teamId))
                .and(hasRegistrationStatus(registrationStatus));
    }

    private static Specification<TournamentTeam> hasTournamentId(Long tournamentId) {
        return (root, query, builder) ->
                tournamentId == null ? builder.conjunction() : builder.equal(root.get("tournamentId"), tournamentId);
    }

    private static Specification<TournamentTeam> hasTeamId(Long teamId) {
        return (root, query, builder) ->
                teamId == null ? builder.conjunction() : builder.equal(root.get("teamId"), teamId);
    }

    private static Specification<TournamentTeam> hasRegistrationStatus(TournamentTeamRegistrationStatus registrationStatus) {
        return (root, query, builder) ->
                registrationStatus == null ? builder.conjunction() : builder.equal(root.get("registrationStatus"), registrationStatus);
    }
}
