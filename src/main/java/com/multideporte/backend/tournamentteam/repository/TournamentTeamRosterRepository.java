package com.multideporte.backend.tournamentteam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentTeamRosterRepository extends JpaRepository<RosterRef, Long> {

    boolean existsByTournamentTeamId(Long tournamentTeamId);
}
