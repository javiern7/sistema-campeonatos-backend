package com.multideporte.backend.matchevent.repository;

import com.multideporte.backend.matchevent.entity.MatchEvent;
import com.multideporte.backend.matchevent.entity.MatchEventStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findAllByMatchIdOrderByEventMinuteAscEventSecondAscCreatedAtAscIdAsc(Long matchId);

    boolean existsByMatchId(Long matchId);

    @Query("""
            select event
            from MatchEvent event
            where event.tournamentId = :tournamentId
              and event.status = :status
              and (:matchId is null or event.matchId = :matchId)
              and (:tournamentTeamId is null or event.tournamentTeamId = :tournamentTeamId)
              and (:playerId is null or event.playerId = :playerId)
            order by event.matchId asc, event.eventMinute asc, event.eventSecond asc, event.createdAt asc, event.id asc
            """)
    List<MatchEvent> findActiveDerivedStatisticsEvents(
            @Param("tournamentId") Long tournamentId,
            @Param("status") MatchEventStatus status,
            @Param("matchId") Long matchId,
            @Param("tournamentTeamId") Long tournamentTeamId,
            @Param("playerId") Long playerId
    );

    @Query("""
            select event
            from MatchEvent event
            where event.tournamentId = :tournamentId
              and event.status = :status
              and (:matchId is null or event.matchId = :matchId)
              and (:tournamentTeamId is null or event.tournamentTeamId = :tournamentTeamId)
              and (:teamId is null
                   or exists (
                       select 1
                       from TournamentTeam tournamentTeam
                       where tournamentTeam.tournamentId = event.tournamentId
                         and tournamentTeam.teamId = :teamId
                         and tournamentTeam.id = event.tournamentTeamId
                   ))
              and (:playerId is null or event.playerId = :playerId)
            order by event.matchId asc, event.eventMinute asc,
                     event.eventSecond asc, event.createdAt asc, event.id asc
            """)
    List<MatchEvent> findReportEvents(
            @Param("tournamentId") Long tournamentId,
            @Param("status") MatchEventStatus status,
            @Param("matchId") Long matchId,
            @Param("tournamentTeamId") Long tournamentTeamId,
            @Param("teamId") Long teamId,
            @Param("playerId") Long playerId
    );
}
