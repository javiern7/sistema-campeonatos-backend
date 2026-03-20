package com.multideporte.backend.team.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_team")
class TeamTournamentRef {

    @Id
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;
}
