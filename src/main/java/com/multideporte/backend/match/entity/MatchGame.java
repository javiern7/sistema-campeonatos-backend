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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Column(name = "home_penalty_score")
    private Integer homePenaltyScore;

    @Column(name = "away_penalty_score")
    private Integer awayPenaltyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_method", length = 20)
    private MatchResolutionMethod resolutionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_purpose", length = 20)
    private MatchPurpose matchPurpose;

    @Column(name = "bracket_position")
    private Integer bracketPosition;

    @Column(name = "home_source_match_id")
    private Long homeSourceMatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "home_source_outcome", length = 10)
    private MatchSourceOutcome homeSourceOutcome;

    @Column(name = "away_source_match_id")
    private Long awaySourceMatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "away_source_outcome", length = 10)
    private MatchSourceOutcome awaySourceOutcome;

    @Column(name = "winner_tournament_team_id")
    private Long winnerTournamentTeamId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
