package com.multideporte.backend.matchevent.repository;

import com.multideporte.backend.matchevent.entity.MatchEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    List<MatchEvent> findAllByMatchIdOrderByEventMinuteAscEventSecondAscCreatedAtAscIdAsc(Long matchId);

    boolean existsByMatchId(Long matchId);
}
