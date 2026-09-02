package com.multideporte.backend.standing.service;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** P56 ordering. It never uses a technical identifier as a sporting tie-breaker. */
@Service
public class P56StandingOrderService {

    public List<Long> order(List<Long> teamIds, List<MatchGame> matches) {
        return rank(new HashSet<>(teamIds), matches, false);
    }

    private List<Long> rank(Set<Long> teams, List<MatchGame> matches, boolean headToHead) {
        Map<Long, Totals> totals = totals(teams, matches);
        Comparator<Long> comparator = Comparator.comparingInt((Long id) -> totals.get(id).points).reversed()
                .thenComparing(Comparator.comparingInt((Long id) -> totals.get(id).difference()).reversed())
                .thenComparing(Comparator.comparingInt((Long id) -> totals.get(id).goalsFor).reversed());
        List<Long> sorted = teams.stream().sorted(comparator).toList();
        List<Long> result = new ArrayList<>();
        int cursor = 0;
        while (cursor < sorted.size()) {
            int end = cursor + 1;
            while (end < sorted.size() && same(totals.get(sorted.get(cursor)), totals.get(sorted.get(end)))) end++;
            Set<Long> tied = new HashSet<>(sorted.subList(cursor, end));
            if (tied.size() == 1) result.addAll(tied);
            else if (headToHead) {
                if (tied.size() == teams.size()) throw new BusinessException("TIEBREAK_DRAW_REQUIRED: persiste empate tras enfrentamientos directos; registre el sorteo auditable");
                result.addAll(rank(tied, onlyInternalMatches(tied, matches), false));
            }
            else result.addAll(rank(tied, onlyInternalMatches(tied, matches), true));
            cursor = end;
        }
        return result;
    }

    private List<MatchGame> onlyInternalMatches(Set<Long> teams, List<MatchGame> matches) {
        return matches.stream().filter(match -> teams.contains(match.getHomeTournamentTeamId())
                && teams.contains(match.getAwayTournamentTeamId())).toList();
    }

    private Map<Long, Totals> totals(Set<Long> teams, List<MatchGame> matches) {
        Map<Long, Totals> result = new HashMap<>();
        teams.forEach(id -> result.put(id, new Totals()));
        for (MatchGame match : matches) {
            Totals home = result.get(match.getHomeTournamentTeamId()); Totals away = result.get(match.getAwayTournamentTeamId());
            if (home == null || away == null || match.getHomeScore() == null || match.getAwayScore() == null) continue;
            home.goalsFor += match.getHomeScore(); home.goalsAgainst += match.getAwayScore();
            away.goalsFor += match.getAwayScore(); away.goalsAgainst += match.getHomeScore();
            if (match.getStatus() == com.multideporte.backend.match.entity.MatchGameStatus.FORFEIT
                    && match.getWinnerTournamentTeamId() != null) {
                if (match.getWinnerTournamentTeamId().equals(match.getHomeTournamentTeamId())) home.points += 3;
                else if (match.getWinnerTournamentTeamId().equals(match.getAwayTournamentTeamId())) away.points += 3;
                continue;
            }
            if (match.getHomeScore() > match.getAwayScore()) home.points += 3;
            else if (match.getHomeScore() < match.getAwayScore()) away.points += 3;
            else { home.points++; away.points++; }
        }
        return result;
    }

    private boolean same(Totals left, Totals right) {
        return left.points == right.points && left.difference() == right.difference() && left.goalsFor == right.goalsFor;
    }

    private static final class Totals {
        private int points; private int goalsFor; private int goalsAgainst;
        private int difference() { return goalsFor - goalsAgainst; }
    }
}
