package com.multideporte.backend.roster.repository;

import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import com.multideporte.backend.roster.entity.RosterStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

    boolean existsByTournamentTeamIdAndCaptainTrueAndRosterStatus(Long tournamentTeamId, RosterStatus rosterStatus);

    boolean existsByTournamentTeamIdAndCaptainTrueAndRosterStatusAndIdNot(
            Long tournamentTeamId,
            RosterStatus rosterStatus,
            Long id
    );
}
