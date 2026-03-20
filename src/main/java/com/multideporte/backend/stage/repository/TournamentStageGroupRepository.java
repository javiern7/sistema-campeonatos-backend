package com.multideporte.backend.stage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentStageGroupRepository extends JpaRepository<StageGroupRef, Long> {

    boolean existsByStageId(Long stageId);
}
