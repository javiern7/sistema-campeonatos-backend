package com.multideporte.backend.stage.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stage_group")
class StageGroupRef {

    @Id
    private Long id;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;
}
