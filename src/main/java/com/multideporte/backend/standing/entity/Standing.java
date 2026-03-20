package com.multideporte.backend.standing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "standing")
@Getter
@Setter
public class Standing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "stage_id")
    private Long stageId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "tournament_team_id", nullable = false)
    private Long tournamentTeamId;

    @Column(name = "played", nullable = false)
    private Integer played;

    @Column(name = "wins", nullable = false)
    private Integer wins;

    @Column(name = "draws", nullable = false)
    private Integer draws;

    @Column(name = "losses", nullable = false)
    private Integer losses;

    @Column(name = "points_for", nullable = false)
    private Integer pointsFor;

    @Column(name = "points_against", nullable = false)
    private Integer pointsAgainst;

    @Column(name = "score_diff", nullable = false)
    private Integer scoreDiff;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
