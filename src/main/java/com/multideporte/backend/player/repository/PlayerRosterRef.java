package com.multideporte.backend.player.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_player_roster")
class PlayerRosterRef {

    @Id
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;
}
