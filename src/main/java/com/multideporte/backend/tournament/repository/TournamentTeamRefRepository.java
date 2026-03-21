package com.multideporte.backend.tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentTeamRefRepository extends JpaRepository<TournamentTeamRef, Long> {

    boolean existsByTournamentId(Long tournamentId);
}
