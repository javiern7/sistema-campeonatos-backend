package com.multideporte.backend.tournament.service.impl;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.entity.MatchPurpose;
import com.multideporte.backend.match.entity.MatchSourceOutcome;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournament.dto.response.PilotKnockoutResponse;
import com.multideporte.backend.tournament.dto.response.PilotFinalClassificationResponse;
import com.multideporte.backend.tournament.service.PilotKnockoutService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PilotKnockoutServiceImpl implements PilotKnockoutService {
    private final TournamentStageRepository stageRepository;
    private final StageGroupRepository groupRepository;
    private final StandingRepository standingRepository;
    private final MatchGameRepository matchRepository;

    @Override
    @Transactional
    public PilotKnockoutResponse createSemifinals(Long tournamentId, Long groupStageId, Long knockoutStageId) {
        validateStage(tournamentId, groupStageId, TournamentStageType.GROUP_STAGE);
        TournamentStage knockout = validateStage(tournamentId, knockoutStageId, TournamentStageType.KNOCKOUT);
        List<MatchGame> existing = matchRepository.findAllByStageIdAndMatchPurpose(knockoutStageId, MatchPurpose.SEMIFINAL);
        if (!existing.isEmpty()) {
            if (existing.size() == 2) return response(tournamentId, knockoutStageId, true, existing);
            throw new BusinessException("DERIVED_MATCH_EXISTS: semifinales existentes incompletas o divergentes");
        }
        List<StageGroup> groups = groupRepository.findAllByStageIdOrderBySequenceOrderAsc(groupStageId);
        if (groups.size() != 2) throw new BusinessException("PILOT_FORMAT_INVALID: se requieren exactamente dos grupos");
        StageGroup groupA = groupByCode(groups, "A"); StageGroup groupB = groupByCode(groups, "B");
        List<Standing> a = completeGroup(tournamentId, groupStageId, groupA);
        List<Standing> b = completeGroup(tournamentId, groupStageId, groupB);
        List<MatchGame> semifinals = List.of(
                knockoutMatch(tournamentId, knockout.getId(), MatchPurpose.SEMIFINAL, 1, a.get(0).getTournamentTeamId(), b.get(1).getTournamentTeamId()),
                knockoutMatch(tournamentId, knockout.getId(), MatchPurpose.SEMIFINAL, 2, b.get(0).getTournamentTeamId(), a.get(1).getTournamentTeamId())
        );
        stageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId).forEach(stage -> stage.setActive(false));
        knockout.setActive(true);
        stageRepository.saveAll(stageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId));
        List<MatchGame> saved = matchRepository.saveAll(semifinals);
        return response(tournamentId, knockoutStageId, false, saved);
    }

    @Override
    @Transactional
    public PilotKnockoutResponse createFinalAndThirdPlace(Long tournamentId, Long knockoutStageId) {
        validateStage(tournamentId, knockoutStageId, TournamentStageType.KNOCKOUT);
        List<MatchGame> existingFinal = matchRepository.findAllByStageIdAndMatchPurpose(knockoutStageId, MatchPurpose.FINAL);
        List<MatchGame> existingThird = matchRepository.findAllByStageIdAndMatchPurpose(knockoutStageId, MatchPurpose.THIRD_PLACE);
        if (!existingFinal.isEmpty() || !existingThird.isEmpty()) {
            if (existingFinal.size() == 1 && existingThird.size() == 1) {
                List<MatchGame> both = new ArrayList<>(existingFinal); both.addAll(existingThird);
                return response(tournamentId, knockoutStageId, true, both);
            }
            throw new BusinessException("DERIVED_MATCH_EXISTS: ronda final existente incompleta o divergente");
        }
        List<MatchGame> semis = matchRepository.findAllByStageIdAndMatchPurpose(knockoutStageId, MatchPurpose.SEMIFINAL)
                .stream().sorted(Comparator.comparing(MatchGame::getBracketPosition)).toList();
        if (semis.size() != 2 || semis.stream().anyMatch(s -> s.getStatus() != MatchGameStatus.PLAYED || s.getWinnerTournamentTeamId() == null)) {
            throw new BusinessException("GROUP_STAGE_INCOMPLETE: ambas semifinales PLAYED con ganador valido son obligatorias");
        }
        MatchGame first = semis.get(0); MatchGame second = semis.get(1);
        MatchGame finalMatch = knockoutMatch(tournamentId, knockoutStageId, MatchPurpose.FINAL, 3,
                first.getWinnerTournamentTeamId(), second.getWinnerTournamentTeamId());
        finalMatch.setHomeSourceMatchId(first.getId()); finalMatch.setHomeSourceOutcome(MatchSourceOutcome.WINNER);
        finalMatch.setAwaySourceMatchId(second.getId()); finalMatch.setAwaySourceOutcome(MatchSourceOutcome.WINNER);
        MatchGame third = knockoutMatch(tournamentId, knockoutStageId, MatchPurpose.THIRD_PLACE, 4,
                loser(first), loser(second));
        third.setHomeSourceMatchId(first.getId()); third.setHomeSourceOutcome(MatchSourceOutcome.LOSER);
        third.setAwaySourceMatchId(second.getId()); third.setAwaySourceOutcome(MatchSourceOutcome.LOSER);
        List<MatchGame> saved = matchRepository.saveAll(List.of(finalMatch, third));
        return response(tournamentId, knockoutStageId, false, saved);
    }

    @Override
    public PilotFinalClassificationResponse finalClassification(Long tournamentId, Long knockoutStageId) {
        validateStage(tournamentId, knockoutStageId, TournamentStageType.KNOCKOUT);
        MatchGame finalMatch = singlePlayed(knockoutStageId, MatchPurpose.FINAL);
        MatchGame thirdMatch = singlePlayed(knockoutStageId, MatchPurpose.THIRD_PLACE);
        return new PilotFinalClassificationResponse(tournamentId, knockoutStageId, List.of(
                new PilotFinalClassificationResponse.Entry(1, finalMatch.getWinnerTournamentTeamId()),
                new PilotFinalClassificationResponse.Entry(2, loser(finalMatch)),
                new PilotFinalClassificationResponse.Entry(3, thirdMatch.getWinnerTournamentTeamId()),
                new PilotFinalClassificationResponse.Entry(4, loser(thirdMatch))
        ));
    }

    private MatchGame singlePlayed(Long stageId, MatchPurpose purpose) {
        List<MatchGame> matches = matchRepository.findAllByStageIdAndMatchPurpose(stageId, purpose);
        if (matches.size() != 1 || matches.get(0).getStatus() != MatchGameStatus.PLAYED
                || matches.get(0).getWinnerTournamentTeamId() == null) {
            throw new BusinessException("FINAL_CLASSIFICATION_INCOMPLETE: final y tercer puesto PLAYED con ganador son obligatorios");
        }
        return matches.get(0);
    }
    private List<Standing> completeGroup(Long tournamentId, Long stageId, StageGroup group) {
        List<MatchGame> played = matchRepository.findAllByTournamentIdAndStageIdAndGroupIdAndStatusIn(tournamentId, stageId, group.getId(), List.of(MatchGameStatus.PLAYED));
        List<Standing> standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(tournamentId, stageId, group.getId());
        if (played.size() != 6 || standings.size() != 4 || standings.stream().anyMatch(s -> s.getRankPosition() == null)) {
            throw new BusinessException("GROUP_STAGE_INCOMPLETE: grupo " + group.getCode() + " requiere seis partidos PLAYED y cuatro standings");
        }
        return standings;
    }

    private MatchGame knockoutMatch(Long tournamentId, Long stageId, MatchPurpose purpose, int position, Long home, Long away) {
        MatchGame match = new MatchGame(); match.setTournamentId(tournamentId); match.setStageId(stageId); match.setRoundNumber(2);
        match.setMatchdayNumber(position); match.setHomeTournamentTeamId(home); match.setAwayTournamentTeamId(away);
        match.setStatus(MatchGameStatus.SCHEDULED); match.setMatchPurpose(purpose); match.setBracketPosition(position); return match;
    }

    private Long loser(MatchGame match) { return match.getWinnerTournamentTeamId().equals(match.getHomeTournamentTeamId()) ? match.getAwayTournamentTeamId() : match.getHomeTournamentTeamId(); }

    private TournamentStage validateStage(Long tournamentId, Long stageId, TournamentStageType type) {
        TournamentStage stage = stageRepository.findById(stageId).orElseThrow(() -> new BusinessException("PILOT_FORMAT_INVALID: etapa inexistente"));
        if (!tournamentId.equals(stage.getTournamentId()) || stage.getStageType() != type) throw new BusinessException("PILOT_FORMAT_INVALID: etapa incompatible");
        return stage;
    }

    private StageGroup groupByCode(List<StageGroup> groups, String code) {
        return groups.stream().filter(g -> code.equalsIgnoreCase(g.getCode())).findFirst()
                .orElseThrow(() -> new BusinessException("PILOT_FORMAT_INVALID: falta grupo " + code));
    }

    private PilotKnockoutResponse response(Long tournamentId, Long stageId, boolean existing, List<MatchGame> matches) {
        return new PilotKnockoutResponse(tournamentId, stageId, existing, matches.stream().map(MatchGame::getId).toList());
    }
}
