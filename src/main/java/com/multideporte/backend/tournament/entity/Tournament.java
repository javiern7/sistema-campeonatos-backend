package com.multideporte.backend.tournament.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tournament")
@Getter
@Setter
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_id", nullable = false)
    private Long sportId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "season_name", nullable = false, length = 50)
    private String seasonName;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 30)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TournamentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_category", nullable = false, length = 20)
    private TournamentOperationalCategory operationalCategory;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "registration_open_at")
    private OffsetDateTime registrationOpenAt;

    @Column(name = "registration_close_at")
    private OffsetDateTime registrationCloseAt;

    @Column(name = "max_teams")
    private Integer maxTeams;

    @Column(name = "points_win", nullable = false)
    private Integer pointsWin;

    @Column(name = "points_draw", nullable = false)
    private Integer pointsDraw;

    @Column(name = "points_loss", nullable = false)
    private Integer pointsLoss;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
