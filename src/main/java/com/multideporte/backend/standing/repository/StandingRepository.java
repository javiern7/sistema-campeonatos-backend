package com.multideporte.backend.standing.repository;

import com.multideporte.backend.standing.entity.Standing;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StandingRepository extends JpaRepository<Standing, Long>, JpaSpecificationExecutor<Standing> {

    boolean existsByStageId(Long stageId);

    boolean existsByGroupId(Long groupId);

    boolean existsByTournamentTeamId(Long tournamentTeamId);

    Optional<Standing> findByTournamentIdAndStageIdAndGroupIdAndTournamentTeamId(
            Long tournamentId,
            Long stageId,
            Long groupId,
            Long tournamentTeamId
    );

    List<Standing> findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(Long tournamentId);

    List<Standing> findAllByTournamentIdAndStageIdAndGroupIdIsNull(Long tournamentId, Long stageId);

    List<Standing> findAllByTournamentIdAndStageIdAndGroupId(Long tournamentId, Long stageId, Long groupId);

    List<Standing> findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(Long tournamentId, Long stageId, Long groupId);
}
