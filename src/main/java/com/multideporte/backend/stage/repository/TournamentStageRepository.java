package com.multideporte.backend.stage.repository;

import com.multideporte.backend.stage.entity.TournamentStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentStageRepository extends JpaRepository<TournamentStage, Long>, JpaSpecificationExecutor<TournamentStage> {

    boolean existsByTournamentIdAndSequenceOrder(Long tournamentId, Integer sequenceOrder);

    boolean existsByTournamentIdAndSequenceOrderAndIdNot(Long tournamentId, Integer sequenceOrder, Long id);

    List<TournamentStage> findAllByTournamentIdOrderBySequenceOrderAsc(Long tournamentId);
}
