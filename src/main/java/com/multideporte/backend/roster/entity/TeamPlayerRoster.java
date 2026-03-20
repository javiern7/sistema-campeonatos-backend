package com.multideporte.backend.roster.entity;

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

@Entity
@Table(name = "team_player_roster")
@Getter
@Setter
public class TeamPlayerRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_team_id", nullable = false)
    private Long tournamentTeamId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(name = "captain", nullable = false)
    private Boolean captain;

    @Column(name = "position_name", length = 50)
    private String positionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "roster_status", nullable = false, length = 20)
    private RosterStatus rosterStatus;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
