package com.multideporte.backend.tournament.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_team")
class TournamentTeamRef {

    @Id
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;
}
