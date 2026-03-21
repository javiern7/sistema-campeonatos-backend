package com.multideporte.backend.tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentStageRefRepository extends JpaRepository<TournamentStageRef, Long> {

    boolean existsByTournamentId(Long tournamentId);
}
