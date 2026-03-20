package com.multideporte.backend.standing.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.standing.service.StandingRecalculationService;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StandingRecalculationServiceImpl implements StandingRecalculationService {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;

    @Override
    public StandingRecalculationResponse recalculate(StandingRecalculateRequest request) {
        validateScope(request);

        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new BusinessException("El tournamentId enviado no existe"));

        List<MatchGame> matches = loadMatches(request);
        List<Standing> currentStandings = loadCurrentStandings(request);

        if (!currentStandings.isEmpty()) {
            standingRepository.deleteAllInBatch(currentStandings);
        }

        Map<Long, StandingAccumulator> table = new HashMap<>();
        for (MatchGame match : matches) {
            accumulateMatch(table, match, tournament);
        }

        List<Standing> standingsToSave = buildStandings(request, table);
        if (!standingsToSave.isEmpty()) {
            standingRepository.saveAll(standingsToSave);
        }

        return new StandingRecalculationResponse(
                request.tournamentId(),
                request.stageId(),
                request.groupId(),
                matches.size(),
                standingsToSave.size()
        );
    }

    private void validateScope(StandingRecalculateRequest request) {
        if (!tournamentRepository.existsById(request.tournamentId())) {
            throw new BusinessException("El tournamentId enviado no existe");
        }

        if (request.groupId() != null && request.stageId() == null) {
            throw new BusinessException("groupId requiere stageId para recalculo");
        }

        if (request.stageId() != null) {
            TournamentStage stage = tournamentStageRepository.findById(request.stageId())
                    .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));

            if (!stage.getTournamentId().equals(request.tournamentId())) {
                throw new BusinessException("El stageId no pertenece al torneo indicado");
            }
        }

        if (request.groupId() != null) {
            StageGroup group = stageGroupRepository.findById(request.groupId())
                    .orElseThrow(() -> new BusinessException("El groupId enviado no existe"));

            if (!group.getStageId().equals(request.stageId())) {
                throw new BusinessException("El groupId no pertenece al stageId indicado");
            }
        }
    }

    private List<MatchGame> loadMatches(StandingRecalculateRequest request) {
        var statuses = EnumSet.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT);
        if (request.groupId() != null) {
            return matchGameRepository.findAllByTournamentIdAndStageIdAndGroupIdAndStatusIn(
                    request.tournamentId(),
                    request.stageId(),
                    request.groupId(),
                    statuses
            );
        }
        if (request.stageId() != null) {
            return matchGameRepository.findAllByTournamentIdAndStageIdAndGroupIdIsNullAndStatusIn(
                    request.tournamentId(),
                    request.stageId(),
                    statuses
            );
        }
        return matchGameRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNullAndStatusIn(
                request.tournamentId(),
                statuses
        );
    }

    private List<Standing> loadCurrentStandings(StandingRecalculateRequest request) {
        if (request.groupId() != null) {
            return standingRepository.findAllByTournamentIdAndStageIdAndGroupId(
                    request.tournamentId(),
                    request.stageId(),
                    request.groupId()
            );
        }
        if (request.stageId() != null) {
            return standingRepository.findAllByTournamentIdAndStageIdAndGroupIdIsNull(
                    request.tournamentId(),
                    request.stageId()
            );
        }
        return standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(request.tournamentId());
    }

    private void accumulateMatch(Map<Long, StandingAccumulator> table, MatchGame match, Tournament tournament) {
        StandingAccumulator home = table.computeIfAbsent(match.getHomeTournamentTeamId(), StandingAccumulator::new);
        StandingAccumulator away = table.computeIfAbsent(match.getAwayTournamentTeamId(), StandingAccumulator::new);

        int homeScore = match.getHomeScore() == null ? 0 : match.getHomeScore();
        int awayScore = match.getAwayScore() == null ? 0 : match.getAwayScore();

        home.played++;
        away.played++;
        home.pointsFor += homeScore;
        home.pointsAgainst += awayScore;
        away.pointsFor += awayScore;
        away.pointsAgainst += homeScore;

        if (match.getStatus() == MatchGameStatus.FORFEIT && match.getWinnerTournamentTeamId() != null) {
            applyWinner(home, away, tournament, match.getWinnerTournamentTeamId(), match.getHomeTournamentTeamId());
        } else if (homeScore > awayScore) {
            home.wins++;
            away.losses++;
            home.points += tournament.getPointsWin();
            away.points += tournament.getPointsLoss();
        } else if (homeScore < awayScore) {
            away.wins++;
            home.losses++;
            away.points += tournament.getPointsWin();
            home.points += tournament.getPointsLoss();
        } else {
            home.draws++;
            away.draws++;
            home.points += tournament.getPointsDraw();
            away.points += tournament.getPointsDraw();
        }
    }

    private void applyWinner(
            StandingAccumulator home,
            StandingAccumulator away,
            Tournament tournament,
            Long winnerTournamentTeamId,
            Long homeTournamentTeamId
    ) {
        boolean homeWon = winnerTournamentTeamId.equals(homeTournamentTeamId);
        if (homeWon) {
            home.wins++;
            away.losses++;
            home.points += tournament.getPointsWin();
            away.points += tournament.getPointsLoss();
            return;
        }

        away.wins++;
        home.losses++;
        away.points += tournament.getPointsWin();
        home.points += tournament.getPointsLoss();
    }

    private List<Standing> buildStandings(StandingRecalculateRequest request, Map<Long, StandingAccumulator> table) {
        List<StandingAccumulator> ordered = new ArrayList<>(table.values());
        ordered.sort(Comparator
                .comparingInt(StandingAccumulator::points).reversed()
                .thenComparingInt(StandingAccumulator::scoreDiff).reversed()
                .thenComparingInt(StandingAccumulator::pointsFor).reversed()
                .thenComparingLong(StandingAccumulator::teamId));

        List<Standing> standings = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            StandingAccumulator acc = ordered.get(i);
            Standing standing = new Standing();
            standing.setTournamentId(request.tournamentId());
            standing.setStageId(request.stageId());
            standing.setGroupId(request.groupId());
            standing.setTournamentTeamId(acc.teamId);
            standing.setPlayed(acc.played);
            standing.setWins(acc.wins);
            standing.setDraws(acc.draws);
            standing.setLosses(acc.losses);
            standing.setPointsFor(acc.pointsFor);
            standing.setPointsAgainst(acc.pointsAgainst);
            standing.setScoreDiff(acc.scoreDiff());
            standing.setPoints(acc.points);
            standing.setRankPosition(i + 1);
            standings.add(standing);
        }
        return standings;
    }

    private static final class StandingAccumulator {
        private final Long teamId;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int pointsFor;
        private int pointsAgainst;
        private int points;

        private StandingAccumulator(Long teamId) {
            this.teamId = teamId;
        }

        private Long teamId() {
            return teamId;
        }

        private int points() {
            return points;
        }

        private int pointsFor() {
            return pointsFor;
        }

        private int scoreDiff() {
            return pointsFor - pointsAgainst;
        }
    }
}
