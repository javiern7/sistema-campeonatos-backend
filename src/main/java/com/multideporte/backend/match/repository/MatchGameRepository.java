package com.multideporte.backend.match.repository;

import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchGameRepository extends JpaRepository<MatchGame, Long>, JpaSpecificationExecutor<MatchGame> {

    List<MatchGame> findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNullAndStatusIn(
            Long tournamentId,
            Collection<MatchGameStatus> statuses
    );

    List<MatchGame> findAllByTournamentIdAndStageIdAndGroupIdIsNullAndStatusIn(
            Long tournamentId,
            Long stageId,
            Collection<MatchGameStatus> statuses
    );

    List<MatchGame> findAllByTournamentIdAndStageIdAndGroupIdAndStatusIn(
            Long tournamentId,
            Long stageId,
            Long groupId,
            Collection<MatchGameStatus> statuses
    );
}
