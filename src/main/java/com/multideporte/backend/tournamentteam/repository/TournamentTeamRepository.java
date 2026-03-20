package com.multideporte.backend.tournamentteam.repository;

import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentTeamRepository extends JpaRepository<TournamentTeam, Long>, JpaSpecificationExecutor<TournamentTeam> {

    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    Optional<TournamentTeam> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    boolean existsByTournamentIdAndSeedNumber(Long tournamentId, Integer seedNumber);

    long countByTournamentIdAndRegistrationStatusIn(
            Long tournamentId,
            Iterable<TournamentTeamRegistrationStatus> statuses
    );
}
