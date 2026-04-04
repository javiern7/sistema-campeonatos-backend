package com.multideporte.backend.tournamentteam.repository;

import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long>, JpaSpecificationExecutor<TournamentTeam> {

    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    Optional<TournamentTeam> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    boolean existsByTournamentIdAndSeedNumber(Long tournamentId, Integer seedNumber);

    long countByTournamentIdAndRegistrationStatusIn(
            Long tournamentId,
            Iterable<TournamentTeamRegistrationStatus> statuses
    );

    long countByTournamentIdAndRegistrationStatus(Long tournamentId, TournamentTeamRegistrationStatus registrationStatus);

    @Query("""
            select count(tt)
            from TournamentTeam tt
            where tt.tournamentId = :tournamentId
              and tt.registrationStatus = com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus.APPROVED
              and exists (
                  select 1
                  from TeamPlayerRoster roster
                  where roster.tournamentTeamId = tt.id
                    and roster.rosterStatus = com.multideporte.backend.roster.entity.RosterStatus.ACTIVE
                    and roster.endDate is null
              )
            """)
    long countApprovedTeamsWithActiveRosterSupport(@Param("tournamentId") Long tournamentId);
}
