package com.multideporte.backend.player.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRosterRepository extends JpaRepository<PlayerRosterRef, Long> {

    boolean existsByPlayerId(Long playerId);
}
