package com.multideporte.backend.roster.repository;

import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import com.multideporte.backend.roster.entity.RosterStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamPlayerRosterRepository extends JpaRepository<TeamPlayerRoster, Long>, JpaSpecificationExecutor<TeamPlayerRoster> {

    boolean existsByTournamentTeamIdAndPlayerIdAndStartDate(Long tournamentTeamId, Long playerId, LocalDate startDate);

    boolean existsByTournamentTeamIdAndPlayerIdAndRosterStatusAndEndDateIsNull(
            Long tournamentTeamId,
            Long playerId,
            RosterStatus rosterStatus
    );

    boolean existsByTournamentTeamIdAndPlayerIdAndRosterStatusAndEndDateIsNullAndIdNot(
            Long tournamentTeamId,
            Long playerId,
            RosterStatus rosterStatus,
            Long id
    );

    boolean existsByTournamentTeamIdAndRosterStatusAndEndDateIsNull(Long tournamentTeamId, RosterStatus rosterStatus);

    boolean existsByTournamentTeamIdAndRosterStatusAndEndDateIsNullAndIdNot(
            Long tournamentTeamId,
            RosterStatus rosterStatus,
            Long id
    );

    boolean existsByTournamentTeamIdAndCaptainTrueAndRosterStatus(Long tournamentTeamId, RosterStatus rosterStatus);

    boolean existsByTournamentTeamIdAndCaptainTrueAndRosterStatusAndIdNot(
            Long tournamentTeamId,
            RosterStatus rosterStatus,
            Long id
    );

    @Query("""
            select count(roster) > 0
            from TeamPlayerRoster roster
            where roster.tournamentTeamId = :tournamentTeamId
              and roster.playerId = :playerId
              and roster.startDate <= :referenceDate
              and (roster.endDate is null or roster.endDate >= :referenceDate)
            """)
    boolean existsEligibleRosterMembership(
            @Param("tournamentTeamId") Long tournamentTeamId,
            @Param("playerId") Long playerId,
            @Param("referenceDate") LocalDate referenceDate
    );
}
