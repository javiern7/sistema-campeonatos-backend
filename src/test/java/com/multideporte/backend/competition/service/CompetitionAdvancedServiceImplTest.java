package com.multideporte.backend.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.multideporte.backend.competition.dto.response.CompetitionAdvancedBracketResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedCalendarResponse;
import com.multideporte.backend.competition.dto.response.CompetitionAdvancedResultsResponse;
import com.multideporte.backend.competition.service.impl.CompetitionAdvancedServiceImpl;
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
class CompetitionAdvancedServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private MatchGameRepository matchGameRepository;

    @Mock
    private TournamentStageRepository tournamentStageRepository;

    @Mock
    private StageGroupRepository stageGroupRepository;

    @Mock
    private TournamentTeamRepository tournamentTeamRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private StandingRepository standingRepository;

    @InjectMocks
    private CompetitionAdvancedServiceImpl service;

    @Test
    void shouldExposeBracketGroupedByRound() {
        Tournament tournament = tournament(1L);
        TournamentStage knockoutStage = stage(20L, "Llaves Finales", TournamentStageType.KNOCKOUT, true);

        MatchGame semifinalA = knockoutMatch(100L, tournament.getId(), knockoutStage.getId(), 1, 1, 11L, 12L);
        MatchGame semifinalB = knockoutMatch(101L, tournament.getId(), knockoutStage.getId(), 1, 1, 13L, 14L);
        MatchGame finalMatch = knockoutMatch(102L, tournament.getId(), knockoutStage.getId(), 2, 2, 11L, 13L);

        mockTournament(tournament, List.of(knockoutStage));
        when(matchGameRepository.findAll(any(Specification.class))).thenReturn(List.of(semifinalA, semifinalB, finalMatch));
        mockTeams();

        CompetitionAdvancedBracketResponse response = service.getBracket(tournament.getId(), null);

        assertEquals(knockoutStage.getId(), response.stageId());
        assertEquals(3, response.totalMatches());
        assertEquals(2, response.rounds().size());
        assertEquals(1, response.rounds().get(0).roundNumber());
        assertEquals(2, response.rounds().get(0).matchesCount());
        assertEquals(2, response.rounds().get(1).roundNumber());
    }

    @Test
    void shouldExposeCalendarOrderedAndFiltered() {
        Tournament tournament = tournament(1L);
        TournamentStage groupStage = stage(10L, "Fase de grupos", TournamentStageType.GROUP_STAGE, true);
        StageGroup group = group(50L, groupStage.getId(), "A", "Grupo A");

        MatchGame scheduled = groupMatch(200L, tournament.getId(), groupStage.getId(), group.getId(), MatchGameStatus.SCHEDULED,
                OffsetDateTime.parse("2026-04-10T15:00:00Z"), 11L, 12L);
        MatchGame played = groupMatch(201L, tournament.getId(), groupStage.getId(), group.getId(), MatchGameStatus.PLAYED,
                OffsetDateTime.parse("2026-04-09T15:00:00Z"), 13L, 14L);

        mockTournament(tournament, List.of(groupStage));
        when(tournamentStageRepository.findById(groupStage.getId())).thenReturn(Optional.of(groupStage));
        when(stageGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(groupStage.getId())).thenReturn(List.of(group));
        when(matchGameRepository.findAll(any(Specification.class))).thenReturn(List.of(scheduled, played));
        mockTeams();

        CompetitionAdvancedCalendarResponse response = service.getCalendar(
                tournament.getId(),
                groupStage.getId(),
                group.getId(),
                null,
                OffsetDateTime.parse("2026-04-09T00:00:00Z"),
                OffsetDateTime.parse("2026-04-11T00:00:00Z")
        );

        assertEquals(2, response.totalMatches());
        assertEquals(1, response.scheduledMatches());
        assertEquals(1, response.closedMatches());
        assertEquals(played.getId(), response.matches().get(0).matchId());
        assertEquals(scheduled.getId(), response.matches().get(1).matchId());
    }

    @Test
    void shouldExposeResultsAlignedWithStandings() {
        Tournament tournament = tournament(1L);
        TournamentStage groupStage = stage(10L, "Fase de grupos", TournamentStageType.GROUP_STAGE, true);
        TournamentStage knockoutStage = stage(20L, "Llaves", TournamentStageType.KNOCKOUT, false);
        StageGroup group = group(50L, groupStage.getId(), "A", "Grupo A");

        MatchGame groupMatch = groupMatch(300L, tournament.getId(), groupStage.getId(), group.getId(), MatchGameStatus.PLAYED,
                OffsetDateTime.parse("2026-04-08T15:00:00Z"), 11L, 12L);
        groupMatch.setWinnerTournamentTeamId(11L);
        groupMatch.setHomeScore(3);
        groupMatch.setAwayScore(1);

        MatchGame knockoutMatch = knockoutMatch(301L, tournament.getId(), knockoutStage.getId(), 1, 1, 13L, 14L);
        knockoutMatch.setStatus(MatchGameStatus.PLAYED);
        knockoutMatch.setWinnerTournamentTeamId(13L);
        knockoutMatch.setHomeScore(2);
        knockoutMatch.setAwayScore(0);

        mockTournament(tournament, List.of(groupStage, knockoutStage));
        when(stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(groupStage.getId())).thenReturn(List.of(group));
        when(stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(knockoutStage.getId())).thenReturn(List.of());
        when(matchGameRepository.findAll(any(Specification.class))).thenReturn(List.of(groupMatch, knockoutMatch));
        when(standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(
                tournament.getId(),
                groupStage.getId(),
                group.getId()
        )).thenReturn(List.of(standing(11L, 1)));
        mockTeams();

        CompetitionAdvancedResultsResponse response = service.getResults(tournament.getId(), null, null);

        assertEquals(2, response.totalClosedMatches());
        CompetitionAdvancedResultsResponse.ResultEntry firstGroupResult = response.results().stream()
                .filter(entry -> entry.match().matchId().equals(groupMatch.getId()))
                .findFirst()
                .orElseThrow();
        CompetitionAdvancedResultsResponse.ResultEntry firstKnockoutResult = response.results().stream()
                .filter(entry -> entry.match().matchId().equals(knockoutMatch.getId()))
                .findFirst()
                .orElseThrow();

        assertTrue(firstGroupResult.affectsStandings());
        assertEquals("GROUP", firstGroupResult.standingScope());
        assertEquals("AVAILABLE", firstGroupResult.standingStatus());
        assertFalse(firstKnockoutResult.affectsStandings());
        assertEquals("NOT_APPLICABLE", firstKnockoutResult.standingScope());
        assertEquals("NOT_APPLICABLE", firstKnockoutResult.standingStatus());
    }

    private void mockTournament(Tournament tournament, List<TournamentStage> stages) {
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournament.getId())).thenReturn(stages);
    }

    private void mockTeams() {
        List<TournamentTeam> tournamentTeams = List.of(
                tournamentTeam(11L, 101L, 1),
                tournamentTeam(12L, 102L, 2),
                tournamentTeam(13L, 103L, 3),
                tournamentTeam(14L, 104L, 4)
        );
        when(tournamentTeamRepository.findAllById(anyCollection())).thenReturn(tournamentTeams);
        when(teamRepository.findAllById(anyCollection())).thenReturn(List.of(
                team(101L, "Halcones"),
                team(102L, "Tigres"),
                team(103L, "Lobos"),
                team(104L, "Pumas")
        ));
    }

    private Tournament tournament(Long id) {
        Tournament tournament = new Tournament();
        tournament.setId(id);
        return tournament;
    }

    private TournamentStage stage(Long id, String name, TournamentStageType type, boolean active) {
        TournamentStage stage = new TournamentStage();
        stage.setId(id);
        stage.setTournamentId(1L);
        stage.setName(name);
        stage.setStageType(type);
        stage.setActive(active);
        stage.setSequenceOrder(type == TournamentStageType.GROUP_STAGE ? 1 : 2);
        return stage;
    }

    private StageGroup group(Long id, Long stageId, String code, String name) {
        StageGroup group = new StageGroup();
        group.setId(id);
        group.setStageId(stageId);
        group.setCode(code);
        group.setName(name);
        return group;
    }

    private MatchGame knockoutMatch(
            Long id,
            Long tournamentId,
            Long stageId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    ) {
        MatchGame match = new MatchGame();
        match.setId(id);
        match.setTournamentId(tournamentId);
        match.setStageId(stageId);
        match.setRoundNumber(roundNumber);
        match.setMatchdayNumber(matchdayNumber);
        match.setHomeTournamentTeamId(homeTournamentTeamId);
        match.setAwayTournamentTeamId(awayTournamentTeamId);
        match.setStatus(MatchGameStatus.SCHEDULED);
        return match;
    }

    private MatchGame groupMatch(
            Long id,
            Long tournamentId,
            Long stageId,
            Long groupId,
            MatchGameStatus status,
            OffsetDateTime scheduledAt,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    ) {
        MatchGame match = new MatchGame();
        match.setId(id);
        match.setTournamentId(tournamentId);
        match.setStageId(stageId);
        match.setGroupId(groupId);
        match.setStatus(status);
        match.setScheduledAt(scheduledAt);
        match.setHomeTournamentTeamId(homeTournamentTeamId);
        match.setAwayTournamentTeamId(awayTournamentTeamId);
        return match;
    }

    private TournamentTeam tournamentTeam(Long id, Long teamId, Integer seedNumber) {
        TournamentTeam tournamentTeam = new TournamentTeam();
        tournamentTeam.setId(id);
        tournamentTeam.setTournamentId(1L);
        tournamentTeam.setTeamId(teamId);
        tournamentTeam.setSeedNumber(seedNumber);
        return tournamentTeam;
    }

    private Team team(Long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        team.setShortName(name);
        team.setCode(name.substring(0, 3).toUpperCase());
        return team;
    }

    private Standing standing(Long tournamentTeamId, Integer rankPosition) {
        Standing standing = new Standing();
        standing.setTournamentTeamId(tournamentTeamId);
        standing.setRankPosition(rankPosition);
        return standing;
    }
}
