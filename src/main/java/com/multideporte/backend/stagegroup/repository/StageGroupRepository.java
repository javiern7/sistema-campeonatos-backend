package com.multideporte.backend.stagegroup.repository;

import com.multideporte.backend.stagegroup.entity.StageGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StageGroupRepository extends JpaRepository<StageGroup, Long>, JpaSpecificationExecutor<StageGroup> {

    boolean existsByStageIdAndCodeIgnoreCase(Long stageId, String code);

    boolean existsByStageIdAndCodeIgnoreCaseAndIdNot(Long stageId, String code, Long id);

    boolean existsByStageIdAndSequenceOrder(Long stageId, Integer sequenceOrder);

    boolean existsByStageIdAndSequenceOrderAndIdNot(Long stageId, Integer sequenceOrder, Long id);

    List<StageGroup> findAllByStageIdOrderBySequenceOrderAsc(Long stageId);
}
