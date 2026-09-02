package com.multideporte.backend.standing.tiebreak.repository;
import com.multideporte.backend.standing.tiebreak.entity.StandingTieBreakDraw; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface StandingTieBreakDrawRepository extends JpaRepository<StandingTieBreakDraw,Long> { Optional<StandingTieBreakDraw> findTopByTournamentIdAndStageIdAndGroupIdOrderByRecordedAtDesc(Long tournamentId, Long stageId, Long groupId); }
