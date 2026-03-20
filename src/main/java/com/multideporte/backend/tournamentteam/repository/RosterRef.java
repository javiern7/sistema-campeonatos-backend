package com.multideporte.backend.tournamentteam.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "team_player_roster")
class RosterRef {

    @Id
    private Long id;

    @Column(name = "tournament_team_id", nullable = false)
    private Long tournamentTeamId;
}
