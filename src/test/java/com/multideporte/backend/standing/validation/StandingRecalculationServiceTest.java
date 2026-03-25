package com.multideporte.backend.standing.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.standing.dto.request.StandingRecalculateRequest;
import com.multideporte.backend.standing.dto.response.StandingRecalculationResponse;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.standing.service.impl.StandingRecalculationServiceImpl;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandingRecalculationServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private com.multideporte.backend.stage.repository.TournamentStageRepository tournamentStageRepository;

    @Mock
    private com.multideporte.backend.stagegroup.repository.StageGroupRepository stageGroupRepository;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private StandingRepository standingRepository;

    @InjectMocks
    private StandingRecalculationServiceImpl standingRecalculationService;

    @Captor
    private ArgumentCaptor<List<com.multideporte.backend.standing.entity.Standing>> standingCaptor;

    @Test
    void shouldRecalculateTournamentLevelStandings() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setPointsWin(3);
        tournament.setPointsDraw(1);
        tournament.setPointsLoss(0);

        MatchGame match = new MatchGame();
        match.setTournamentId(1L);
        match.setHomeTournamentTeamId(10L);
        match.setAwayTournamentTeamId(11L);
        match.setStatus(MatchGameStatus.PLAYED);
        match.setHomeScore(2);
        match.setAwayScore(1);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchGameRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNullAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), anyCollection()
        )).thenReturn(List.of(match));
        when(standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(1L)).thenReturn(List.of());
        when(standingRepository.saveAll(standingCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        StandingRecalculationResponse response = standingRecalculationService.recalculate(
                new StandingRecalculateRequest(1L, null, null)
        );

        assertEquals(1, response.matchesProcessed());
        assertEquals(2, response.standingsGenerated());
        List<com.multideporte.backend.standing.entity.Standing> saved = standingCaptor.getValue();
        assertEquals(2, saved.size());
        assertEquals(10L, saved.get(0).getTournamentTeamId());
        assertEquals(1, saved.get(0).getRankPosition());
        assertEquals(3, saved.get(0).getPoints());
    }

    @Test
    void shouldFailWhenStageDoesNotBelongToTournament() {
        TournamentStage stage = new TournamentStage();
        stage.setId(5L);
        stage.setTournamentId(2L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(5L)).thenReturn(Optional.of(stage));

        Assertions.assertThrows(
                com.multideporte.backend.common.exception.BusinessException.class,
                () -> standingRecalculationService.recalculate(new StandingRecalculateRequest(1L, 5L, null))
        );
    }

    @Test
    void shouldFailWhenGroupDoesNotBelongToStage() {
        TournamentStage stage = new TournamentStage();
        stage.setId(5L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(6L);
        group.setStageId(99L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentStageRepository.findById(5L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(6L)).thenReturn(Optional.of(group));

        Assertions.assertThrows(
                com.multideporte.backend.common.exception.BusinessException.class,
                () -> standingRecalculationService.recalculate(new StandingRecalculateRequest(1L, 5L, 6L))
        );
    }

    @Test
    void shouldUseWinnerWhenForfeitIsRecalculated() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setPointsWin(3);
        tournament.setPointsDraw(1);
        tournament.setPointsLoss(0);

        MatchGame match = new MatchGame();
        match.setTournamentId(1L);
        match.setHomeTournamentTeamId(10L);
        match.setAwayTournamentTeamId(11L);
        match.setStatus(MatchGameStatus.FORFEIT);
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setWinnerTournamentTeamId(11L);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchGameRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNullAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), anyCollection()
        )).thenReturn(List.of(match));
        when(standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(1L)).thenReturn(List.of());
        when(standingRepository.saveAll(standingCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        StandingRecalculationResponse response = standingRecalculationService.recalculate(
                new StandingRecalculateRequest(1L, null, null)
        );

        assertEquals(1, response.matchesProcessed());
        List<com.multideporte.backend.standing.entity.Standing> saved = standingCaptor.getValue();
        assertEquals(11L, saved.get(0).getTournamentTeamId());
        assertEquals(3, saved.get(0).getPoints());
        assertEquals(10L, saved.get(1).getTournamentTeamId());
        assertEquals(0, saved.get(1).getPoints());
    }

    @Test
    void shouldOrderStandingsByPointsThenScoreDiffThenPointsFor() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setPointsWin(3);
        tournament.setPointsDraw(1);
        tournament.setPointsLoss(0);

        MatchGame match1 = new MatchGame();
        match1.setTournamentId(1L);
        match1.setHomeTournamentTeamId(10L);
        match1.setAwayTournamentTeamId(11L);
        match1.setStatus(MatchGameStatus.PLAYED);
        match1.setHomeScore(3);
        match1.setAwayScore(0);

        MatchGame match2 = new MatchGame();
        match2.setTournamentId(1L);
        match2.setHomeTournamentTeamId(12L);
        match2.setAwayTournamentTeamId(10L);
        match2.setStatus(MatchGameStatus.PLAYED);
        match2.setHomeScore(1);
        match2.setAwayScore(0);

        MatchGame match3 = new MatchGame();
        match3.setTournamentId(1L);
        match3.setHomeTournamentTeamId(11L);
        match3.setAwayTournamentTeamId(12L);
        match3.setStatus(MatchGameStatus.PLAYED);
        match3.setHomeScore(2);
        match3.setAwayScore(0);

        when(tournamentRepository.existsById(1L)).thenReturn(true);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(matchGameRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNullAndStatusIn(
                org.mockito.ArgumentMatchers.eq(1L), anyCollection()
        )).thenReturn(List.of(match1, match2, match3));
        when(standingRepository.findAllByTournamentIdAndStageIdIsNullAndGroupIdIsNull(1L)).thenReturn(List.of());
        when(standingRepository.saveAll(standingCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        standingRecalculationService.recalculate(new StandingRecalculateRequest(1L, null, null));

        List<com.multideporte.backend.standing.entity.Standing> saved = standingCaptor.getValue();
        assertEquals(List.of(10L, 11L, 12L), saved.stream()
                .map(com.multideporte.backend.standing.entity.Standing::getTournamentTeamId)
                .toList());
        assertEquals(List.of(1, 2, 3), saved.stream()
                .map(com.multideporte.backend.standing.entity.Standing::getRankPosition)
                .toList());
    }
}
