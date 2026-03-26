package com.multideporte.backend.stage.repository;

import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentStageRepository extends JpaRepository<TournamentStage, Long>, JpaSpecificationExecutor<TournamentStage> {

    boolean existsByTournamentIdAndSequenceOrder(Long tournamentId, Integer sequenceOrder);

    boolean existsByTournamentIdAndSequenceOrderAndIdNot(Long tournamentId, Integer sequenceOrder, Long id);

    boolean existsByTournamentIdAndActiveTrue(Long tournamentId);

    boolean existsByTournamentIdAndActiveTrueAndIdNot(Long tournamentId, Long id);

    List<TournamentStage> findAllByTournamentIdAndActiveTrueOrderBySequenceOrderAsc(Long tournamentId);

    List<TournamentStage> findAllByTournamentIdAndStageTypeOrderBySequenceOrderAsc(Long tournamentId, TournamentStageType stageType);

    List<TournamentStage> findAllByTournamentIdOrderBySequenceOrderAsc(Long tournamentId);
}
