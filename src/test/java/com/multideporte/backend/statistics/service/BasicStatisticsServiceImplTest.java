package com.multideporte.backend.statistics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.statistics.dto.response.BasicStatisticsResponse;
import com.multideporte.backend.statistics.service.impl.BasicStatisticsServiceImpl;
import com.multideporte.backend.team.entity.Team;
import com.multideporte.backend.team.repository.TeamRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BasicStatisticsServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentStageRepository tournamentStageRepository;

    @Mock
    private StageGroupRepository stageGroupRepository;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private StandingRepository standingRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private BasicStatisticsServiceImpl basicStatisticsService;

    @Test
    void shouldBuildSummaryAndLeadersFromStandings() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);
        stage.setStageType(TournamentStageType.LEAGUE);

        MatchGame played = new MatchGame();
        played.setStatus(MatchGameStatus.PLAYED);
        played.setHomeScore(3);
        played.setAwayScore(1);
        played.setScheduledAt(OffsetDateTime.parse("2026-04-08T10:00:00Z"));

        MatchGame scheduled = new MatchGame();
        scheduled.setStatus(MatchGameStatus.SCHEDULED);

        Standing first = new Standing();
        first.setTournamentTeamId(10L);
        first.setRankPosition(1);
        first.setPoints(6);
        first.setWins(2);
        first.setScoreDiff(4);
        first.setPointsFor(5);

        Standing second = new Standing();
        second.setTournamentTeamId(11L);
        second.setRankPosition(2);
        second.setPoints(3);
        second.setWins(1);
        second.setScoreDiff(1);
        second.setPointsFor(2);

        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(10L);
        tournamentTeam.setTeamId(100L);
        tournamentTeam.setSeedNumber(1);

        Team team = new Team();
        team.setId(100L);
        team.setName("Alpha FC");
        team.setShortName("Alpha");
        team.setCode("ALP");

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(matchGameRepository.findAll(org.mockito.ArgumentMatchers.<Specification<MatchGame>>any()))
                .thenReturn(List.of(played, scheduled));
        when(tournamentTeamRepository.countApprovedTeamsWithActiveRosterSupport(1L)).thenReturn(2L);
        when(standingRepository.findAllByTournamentIdAndStageIdAndGroupIdIsNull(1L, 3L)).thenReturn(List.of(second, first));
        when(tournamentTeamRepository.findAllById(anyIterable())).thenReturn(List.of(
                tournamentTeam,
                buildTournamentTeam(11L, 101L, 2)
        ));
        when(teamRepository.findAllById(anyIterable())).thenReturn(List.of(
                team,
                buildTeam(101L, "Beta FC", "Beta", "BET")
        ));

        BasicStatisticsResponse response = basicStatisticsService.getBasicStatistics(1L, 3L, null);

        assertEquals(2, response.summary().totalMatches());
        assertEquals(1, response.summary().playedMatches());
        assertEquals(1, response.summary().scheduledMatches());
        assertEquals(4, response.summary().scoredPointsFor());
        assertEquals("AVAILABLE", response.leaders().pointsLeader().status());
        assertEquals(10L, response.leaders().pointsLeader().team().tournamentTeamId());
        assertEquals("STANDINGS", response.traceability().classificationSource());
    }

    @Test
    void shouldReturnPendingWhenComparableScopeHasNoStandings() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);
        stage.setStageType(TournamentStageType.GROUP_STAGE);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(matchGameRepository.findAll(org.mockito.ArgumentMatchers.<Specification<MatchGame>>any())).thenReturn(List.of());
        when(tournamentTeamRepository.countApprovedTeamsWithActiveRosterSupport(1L)).thenReturn(0L);
        when(standingRepository.findAllByTournamentIdAndStageIdAndGroupIdIsNull(1L, 3L)).thenReturn(List.of());

        BasicStatisticsResponse response = basicStatisticsService.getBasicStatistics(1L, 3L, null);

        assertEquals("PENDING_RECALCULATION", response.leaders().pointsLeader().status());
        assertEquals("STANDINGS_PENDING", response.traceability().classificationSource());
    }

    @Test
    void shouldReturnNotApplicableForKnockoutLeaders() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(9L);
        stage.setTournamentId(1L);
        stage.setStageType(TournamentStageType.KNOCKOUT);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentStageRepository.findById(9L)).thenReturn(Optional.of(stage));
        when(matchGameRepository.findAll(org.mockito.ArgumentMatchers.<Specification<MatchGame>>any())).thenReturn(List.of());
        when(tournamentTeamRepository.countApprovedTeamsWithActiveRosterSupport(1L)).thenReturn(0L);

        BasicStatisticsResponse response = basicStatisticsService.getBasicStatistics(1L, 9L, null);

        assertEquals("NOT_APPLICABLE", response.leaders().pointsLeader().status());
        assertEquals("NOT_APPLICABLE", response.traceability().classificationSource());
    }

    @Test
    void shouldRejectGroupWithoutStage() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(BusinessException.class, () -> basicStatisticsService.getBasicStatistics(1L, null, 7L));
    }

    @Test
    void shouldRejectGroupOutsideStage() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        TournamentStage stage = new TournamentStage();
        stage.setId(3L);
        stage.setTournamentId(1L);

        StageGroup group = new StageGroup();
        group.setId(7L);
        group.setStageId(99L);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(tournamentStageRepository.findById(3L)).thenReturn(Optional.of(stage));
        when(stageGroupRepository.findById(7L)).thenReturn(Optional.of(group));

        assertThrows(BusinessException.class, () -> basicStatisticsService.getBasicStatistics(1L, 3L, 7L));
    }

    private TournamentTeam buildTournamentTeam(Long id, Long teamId, Integer seedNumber) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setTeamId(teamId);
        tournamentTeam.setSeedNumber(seedNumber);
        return tournamentTeam;
    }

    private Team buildTeam(Long id, String name, String shortName, String code) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setShortName(shortName);
        team.setCode(code);
        return team;
    }
}
