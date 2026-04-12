package com.multideporte.backend.matchevent.entity;

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
@Table(name = "match_event")
@Getter
@Setter
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private MatchEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchEventStatus status;

    @Column(name = "tournament_team_id")
    private Long tournamentTeamId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "related_player_id")
    private Long relatedPlayerId;

    @Column(name = "period_label", length = 40)
    private String periodLabel;

    @Column(name = "event_minute")
    private Integer eventMinute;

    @Column(name = "event_second")
    private Integer eventSecond;

    @Column(name = "event_value")
    private Integer eventValue;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "annulled_by_user_id")
    private Long annulledByUserId;

    @Column(name = "annulled_at")
    private OffsetDateTime annulledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
