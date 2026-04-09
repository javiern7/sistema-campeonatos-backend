package com.multideporte.backend.discipline.entity;

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

@Entity
@Table(name = "disciplinary_sanction")
@Getter
@Setter
public class DisciplinarySanction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "tournament_team_id", nullable = false)
    private Long tournamentTeamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sanction_type", nullable = false, length = 40)
    private DisciplinarySanctionType sanctionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DisciplinarySanctionStatus status;

    @Column(name = "matches_to_serve", nullable = false)
    private Integer matchesToServe;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;
}
