package com.multideporte.backend.tournamentteam.entity;

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
@Table(name = "tournament_team")
@Getter
@Setter
public class TournamentTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private TournamentTeamRegistrationStatus registrationStatus;

    @Column(name = "seed_number")
    private Integer seedNumber;

    @Column(name = "group_draw_position")
    private Integer groupDrawPosition;

    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;
}
