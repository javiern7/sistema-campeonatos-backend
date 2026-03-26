package com.multideporte.backend.tournament.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
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
import com.multideporte.backend.tournament.dto.request.KnockoutSeedingStrategy;
import com.multideporte.backend.tournament.dto.request.TournamentKnockoutBracketGenerateRequest;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutBracketResponse;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.service.impl.TournamentStageProgressionServiceImpl;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentStageProgressionServiceTest {

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

    @InjectMocks
    private TournamentStageProgressionServiceImpl service;

    @Test
    void shouldGenerateBracketUsingDefaultGroupRankStrategy() {
        Tournament tournament = tournament();
        TournamentStage activeKnockoutStage = knockoutStage(20L, true);
        TournamentStage previousGroupStage = groupStage(10L);

        mockBracketReadModel(activeKnockoutStage, previousGroupStage, List.of(
                qualifiedGroup(101L, 1001L, 1, 4),
                qualifiedGroup(102L, 1002L, 2, 1),
                qualifiedGroup(103L, 1003L, 3, 3),
                qualifiedGroup(104L, 1004L, 4, 2)
        ));
        when(matchGameRepository.saveAll(anyList())).thenAnswer(invocation -> assignIds(invocation.getArgument(0)));

        TournamentKnockoutBracketResponse response = service.generateKnockoutBracket(
                tournament,
                new TournamentKnockoutBracketGenerateRequest(null, null, null)
        );

        assertEquals(KnockoutSeedingStrategy.GROUP_RANK, response.seedingStrategy());
        assertEquals(2, response.generatedMatchesCount());
        assertEquals(1001L, response.generatedMatches().get(0).homeTournamentTeamId());
        assertEquals(1004L, response.generatedMatches().get(0).awayTournamentTeamId());
        assertEquals(1002L, response.generatedMatches().get(1).homeTournamentTeamId());
        assertEquals(1003L, response.generatedMatches().get(1).awayTournamentTeamId());
    }

    @Test
    void shouldGenerateBracketUsingTournamentSeedStrategy() {
        Tournament tournament = tournament();
        TournamentStage activeKnockoutStage = knockoutStage(20L, true);
        TournamentStage previousGroupStage = groupStage(10L);

        mockBracketReadModel(activeKnockoutStage, previousGroupStage, List.of(
                qualifiedGroup(101L, 1001L, 1, 4),
                qualifiedGroup(102L, 1002L, 2, 1),
                qualifiedGroup(103L, 1003L, 3, 3),
                qualifiedGroup(104L, 1004L, 4, 2)
        ));
        when(matchGameRepository.saveAll(anyList())).thenAnswer(invocation -> assignIds(invocation.getArgument(0)));

        TournamentKnockoutBracketResponse response = service.generateKnockoutBracket(
                tournament,
                new TournamentKnockoutBracketGenerateRequest(KnockoutSeedingStrategy.TOURNAMENT_SEED, 1, 1)
        );

        assertEquals(KnockoutSeedingStrategy.TOURNAMENT_SEED, response.seedingStrategy());
        assertEquals(1002L, response.generatedMatches().get(0).homeTournamentTeamId());
        assertEquals(1001L, response.generatedMatches().get(0).awayTournamentTeamId());
        assertEquals(1004L, response.generatedMatches().get(1).homeTournamentTeamId());
        assertEquals(1003L, response.generatedMatches().get(1).awayTournamentTeamId());
    }

    @Test
    void shouldRejectGeneratingBracketWhenKnockoutStageAlreadyHasMatches() {
        Tournament tournament = tournament();
        TournamentStage activeKnockoutStage = knockoutStage(20L, true);

        when(tournamentStageRepository.findAllByTournamentIdAndActiveTrueOrderBySequenceOrderAsc(1L))
                .thenReturn(List.of(activeKnockoutStage));
        when(matchGameRepository.existsByStageId(20L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.generateKnockoutBracket(
                tournament,
                new TournamentKnockoutBracketGenerateRequest(null, null, null)
        ));
    }

    private void mockBracketReadModel(
            TournamentStage activeKnockoutStage,
            TournamentStage previousGroupStage,
            List<QualifiedGroupFixture> qualifiedGroups
    ) {
        when(tournamentStageRepository.findAllByTournamentIdAndActiveTrueOrderBySequenceOrderAsc(1L))
                .thenReturn(List.of(activeKnockoutStage));
        when(tournamentStageRepository.findById(activeKnockoutStage.getId()))
                .thenReturn(Optional.of(activeKnockoutStage));
        when(matchGameRepository.existsByStageId(activeKnockoutStage.getId())).thenReturn(false);
        when(tournamentStageRepository.findAllByTournamentIdAndStageTypeOrderBySequenceOrderAsc(1L, TournamentStageType.GROUP_STAGE))
                .thenReturn(List.of(previousGroupStage));

        List<StageGroup> groups = qualifiedGroups.stream()
                .map(group -> stageGroup(group.groupId(), previousGroupStage.getId(), group.groupOrder()))
                .toList();
        when(stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(previousGroupStage.getId())).thenReturn(groups);

        for (QualifiedGroupFixture group : qualifiedGroups) {
            when(matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndStatus(
                    1L,
                    previousGroupStage.getId(),
                    group.groupId(),
                    MatchGameStatus.SCHEDULED
            )).thenReturn(false);
            when(matchGameRepository.findAllByTournamentIdAndStageIdAndGroupIdAndStatusIn(
                    1L,
                    previousGroupStage.getId(),
                    group.groupId(),
                    EnumSet.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT)
            )).thenReturn(List.of(completedMatch(group.qualifiedTournamentTeamId())));
            when(standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(
                    1L,
                    previousGroupStage.getId(),
                    group.groupId()
            )).thenReturn(List.of(standing(group.qualifiedTournamentTeamId(), 1)));
            when(tournamentTeamRepository.findById(group.qualifiedTournamentTeamId()))
                    .thenReturn(Optional.of(tournamentTeam(group.qualifiedTournamentTeamId(), group.registrationSeed())));
        }
    }

    private List<MatchGame> assignIds(List<MatchGame> matches) {
        long id = 500L;
        for (MatchGame match : matches) {
            match.setId(id++);
        }
        return matches;
    }

    private Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setFormat(TournamentFormat.GROUPS_THEN_KNOCKOUT);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        return tournament;
    }

    private TournamentStage groupStage(Long id) {
        TournamentStage stage = new TournamentStage();
        stage.setId(id);
        stage.setTournamentId(1L);
        stage.setSequenceOrder(1);
        stage.setStageType(TournamentStageType.GROUP_STAGE);
        stage.setActive(false);
        return stage;
    }

    private TournamentStage knockoutStage(Long id, boolean active) {
        TournamentStage stage = new TournamentStage();
        stage.setId(id);
        stage.setTournamentId(1L);
        stage.setSequenceOrder(2);
        stage.setStageType(TournamentStageType.KNOCKOUT);
        stage.setActive(active);
        return stage;
    }

    private StageGroup stageGroup(Long id, Long stageId, Integer order) {
        StageGroup group = new StageGroup();
        group.setId(id);
        group.setStageId(stageId);
        group.setSequenceOrder(order);
        return group;
    }

    private Standing standing(Long tournamentTeamId, Integer rankPosition) {
        Standing standing = new Standing();
        standing.setTournamentTeamId(tournamentTeamId);
        standing.setRankPosition(rankPosition);
        return standing;
    }

    private TournamentTeam tournamentTeam(Long tournamentTeamId, Integer seedNumber) {
        TournamentTeam team = new TournamentTeam();
        team.setId(tournamentTeamId);
        team.setTournamentId(1L);
        team.setSeedNumber(seedNumber);
        return team;
    }

    private MatchGame completedMatch(Long winnerTournamentTeamId) {
        MatchGame match = new MatchGame();
        match.setTournamentId(1L);
        match.setStatus(MatchGameStatus.PLAYED);
        match.setWinnerTournamentTeamId(winnerTournamentTeamId);
        return match;
    }

    private QualifiedGroupFixture qualifiedGroup(
            Long groupId,
            Long qualifiedTournamentTeamId,
            Integer groupOrder,
            Integer registrationSeed
    ) {
        return new QualifiedGroupFixture(groupId, qualifiedTournamentTeamId, groupOrder, registrationSeed);
    }

    private record QualifiedGroupFixture(
            Long groupId,
            Long qualifiedTournamentTeamId,
            Integer groupOrder,
            Integer registrationSeed
    ) {
    }
}
