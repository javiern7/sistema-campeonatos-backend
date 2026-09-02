package com.multideporte.backend.tournamentpublication.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tournament_publication")
@Getter @Setter
public class TournamentPublication {
    @Id
    @Column(name = "tournament_id")
    private Long tournamentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false)
    private TournamentPublicationStatus publicationStatus;
    @Column(name = "access_mode", nullable = false)
    private String accessMode;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
    @Column(name = "unpublished_at")
    private OffsetDateTime unpublishedAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
}
