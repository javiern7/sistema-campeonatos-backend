package com.multideporte.backend.match.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "match_game")
@Getter
@Setter
public class MatchGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "matchday_number")
    private Integer matchdayNumber;

    @Column(name = "home_tournament_team_id", nullable = false)
    private Long homeTournamentTeamId;

    @Column(name = "away_tournament_team_id", nullable = false)
    private Long awayTournamentTeamId;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "venue_name", length = 150)
    private String venueName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchGameStatus status;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "winner_tournament_team_id")
    private Long winnerTournamentTeamId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
