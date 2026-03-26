package com.multideporte.backend.match.repository;

import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchGameRepository extends JpaRepository<MatchGame, Long>, JpaSpecificationExecutor<MatchGame> {

    boolean existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamId(
            Long tournamentId,
            Long stageId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    );

    boolean existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamIdAndIdNot(
            Long tournamentId,
            Long stageId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            Long id
    );

    boolean existsByStageId(Long stageId);

    boolean existsByGroupId(Long groupId);

    boolean existsByTournamentIdAndStatus(Long tournamentId, MatchGameStatus status);

    boolean existsByHomeTournamentTeamIdOrAwayTournamentTeamIdOrWinnerTournamentTeamId(
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            Long winnerTournamentTeamId
    );

    long countByTournamentIdAndStatusIn(Long tournamentId, Collection<MatchGameStatus> statuses);

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
