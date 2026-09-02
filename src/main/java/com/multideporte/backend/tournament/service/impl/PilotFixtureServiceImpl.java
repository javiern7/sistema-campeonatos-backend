package com.multideporte.backend.tournament.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.dto.request.PilotGroupAssignmentRequest;
import com.multideporte.backend.tournament.dto.response.PilotFixtureResponse;
import com.multideporte.backend.tournament.service.PilotFixtureService;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.entity.TournamentTeamRegistrationStatus;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PilotFixtureServiceImpl implements PilotFixtureService {
    private final TournamentStageRepository stageRepository;
    private final StageGroupRepository groupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchGameRepository matchRepository;

    @Override
    @Transactional
    public void assignGroup(Long tournamentId, Long stageId, Long groupId, PilotGroupAssignmentRequest request) {
        StageGroup group = groupRepository.findById(groupId).orElseThrow(() -> new BusinessException("PILOT_FORMAT_INVALID: grupo inexistente"));
        if (!group.getStageId().equals(stageId) || !isPilotGroup(group.getCode())) {
            throw new BusinessException("PILOT_FORMAT_INVALID: se requieren grupos A/B de la etapa indicada");
        }
        TournamentStage stage = loadGroupStage(tournamentId, stageId);
        if (matchRepository.existsByGroupId(groupId)) {
            throw new BusinessException("OPERATION_CONFLICT: no se puede reasignar un grupo con fixture existente");
        }
        Set<Long> uniqueIds = new HashSet<>(request.tournamentTeamIds());
        if (uniqueIds.size() != 4) throw new BusinessException("PILOT_FORMAT_INVALID: cada grupo requiere cuatro equipos distintos");
        List<TournamentTeam> teams = tournamentTeamRepository.findAllById(request.tournamentTeamIds());
        if (teams.size() != 4 || teams.stream().anyMatch(t -> !tournamentId.equals(t.getTournamentId())
                || t.getRegistrationStatus() != TournamentTeamRegistrationStatus.APPROVED)) {
            throw new BusinessException("PILOT_FORMAT_INVALID: los cuatro equipos deben ser APPROVED y pertenecer al torneo");
        }
        for (int i = 0; i < request.tournamentTeamIds().size(); i++) {
            Long id = request.tournamentTeamIds().get(i);
            TournamentTeam team = teams.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            team.setGroupId(groupId);
            team.setGroupDrawPosition(i + 1);
        }
        tournamentTeamRepository.saveAll(teams);
    }

    @Override
    @Transactional
    public PilotFixtureResponse generate(Long tournamentId, Long stageId) {
        loadGroupStage(tournamentId, stageId);
        List<StageGroup> groups = groupRepository.findAllByStageIdOrderBySequenceOrderAsc(stageId);
        if (groups.size() != 2 || !isPilotGroup(groups.get(0).getCode()) || !isPilotGroup(groups.get(1).getCode())) {
            throw new BusinessException("PILOT_FORMAT_INVALID: se requieren exactamente grupos A y B");
        }
        List<MatchGame> existing = matchRepository.findAllByTournamentIdAndStageId(tournamentId, stageId);
        if (!existing.isEmpty()) {
            if (existing.size() == 12 && existing.stream().allMatch(m -> m.getGroupId() != null)) {
                return new PilotFixtureResponse(tournamentId, stageId, 12, true, existing.stream().map(MatchGame::getId).toList());
            }
            throw new BusinessException("FIXTURE_ALREADY_EXISTS: existe un fixture divergente o incompleto");
        }
        List<MatchGame> fixture = new ArrayList<>();
        for (StageGroup group : groups) fixture.addAll(buildGroupFixture(tournamentId, stageId, group));
        List<MatchGame> saved = matchRepository.saveAll(fixture);
        return new PilotFixtureResponse(tournamentId, stageId, saved.size(), false, saved.stream().map(MatchGame::getId).toList());
    }

    private List<MatchGame> buildGroupFixture(Long tournamentId, Long stageId, StageGroup group) {
        List<TournamentTeam> teams = tournamentTeamRepository.findAllByGroupIdOrderByGroupDrawPositionAsc(group.getId());
        if (teams.size() != 4 || teams.stream().anyMatch(t -> !tournamentId.equals(t.getTournamentId()) || t.getGroupDrawPosition() == null)) {
            throw new BusinessException("PILOT_FORMAT_INVALID: cada grupo requiere cuatro equipos sorteados");
        }
        int[][] pairings = {{0, 3}, {1, 2}, {0, 2}, {3, 1}, {0, 1}, {2, 3}};
        List<MatchGame> matches = new ArrayList<>();
        for (int index = 0; index < pairings.length; index++) {
            MatchGame match = new MatchGame();
            match.setTournamentId(tournamentId); match.setStageId(stageId); match.setGroupId(group.getId());
            match.setRoundNumber(1); match.setMatchdayNumber(index / 2 + 1);
            match.setHomeTournamentTeamId(teams.get(pairings[index][0]).getId());
            match.setAwayTournamentTeamId(teams.get(pairings[index][1]).getId());
            match.setStatus(MatchGameStatus.SCHEDULED); matches.add(match);
        }
        return matches;
    }

    private TournamentStage loadGroupStage(Long tournamentId, Long stageId) {
        TournamentStage stage = stageRepository.findById(stageId).orElseThrow(() -> new BusinessException("PILOT_FORMAT_INVALID: etapa inexistente"));
        if (!tournamentId.equals(stage.getTournamentId()) || stage.getStageType() != TournamentStageType.GROUP_STAGE) {
            throw new BusinessException("PILOT_FORMAT_INVALID: se requiere una etapa GROUP_STAGE del torneo");
        }
        return stage;
    }

    private boolean isPilotGroup(String code) { return "A".equalsIgnoreCase(code) || "B".equalsIgnoreCase(code); }
}
