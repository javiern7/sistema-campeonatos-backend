package com.multideporte.backend.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamTournamentRepository extends JpaRepository<TeamTournamentRef, Long> {

    boolean existsByTeamId(Long teamId);
}
