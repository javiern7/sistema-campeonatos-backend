package com.multideporte.backend.match.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.match.entity.MatchGame;
import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.repository.MatchGameRepository;
import com.multideporte.backend.stage.entity.TournamentStage;
import com.multideporte.backend.stage.entity.TournamentStageType;
import com.multideporte.backend.stage.repository.TournamentStageRepository;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import com.multideporte.backend.stagegroup.repository.StageGroupRepository;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournament.service.TournamentLifecycleGuardService;
import com.multideporte.backend.tournament.service.TournamentStageProgressionService;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchGameValidator {

    private final TournamentRepository tournamentRepository;
    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final MatchGameRepository matchGameRepository;
    private final TournamentLifecycleGuardService tournamentLifecycleGuardService;
    private final TournamentStageProgressionService tournamentStageProgressionService;

    public void validateForCreate(
            Long tournamentId,
            Long stageId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            MatchGameStatus status,
            Integer homeScore,
            Integer awayScore,
            Long winnerTournamentTeamId
    ) {
        Tournament tournament = loadTournament(tournamentId);
        tournamentLifecycleGuardService.assertMatchCanBeManaged(tournament, status);
        TournamentStage stage = validateStageAndGroup(tournamentId, stageId, groupId);
        TournamentTeam home = loadTournamentTeam(homeTournamentTeamId);
        TournamentTeam away = loadTournamentTeam(awayTournamentTeamId);
        validateTeamsBelongToTournament(tournamentId, home, away);
        tournamentStageProgressionService.assertMatchStageCanBeManaged(
                tournament,
                stageId,
                groupId,
                homeTournamentTeamId,
                awayTournamentTeamId
        );
        validateKnockoutCross(stage, null, tournamentId, groupId, roundNumber, matchdayNumber, homeTournamentTeamId, awayTournamentTeamId);
        validateDuplicateMatch(null, tournamentId, stageId, groupId, roundNumber, matchdayNumber, homeTournamentTeamId, awayTournamentTeamId);
        validateScoresAndWinner(stage, homeTournamentTeamId, awayTournamentTeamId, status, homeScore, awayScore, winnerTournamentTeamId);
    }

    public void validateForUpdate(
            MatchGame current,
            Long stageId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            MatchGameStatus status,
            Integer homeScore,
            Integer awayScore,
            Long winnerTournamentTeamId
    ) {
        Tournament tournament = loadTournament(current.getTournamentId());
        tournamentLifecycleGuardService.assertMatchCanBeManaged(tournament, status);
        TournamentStage stage = validateStageAndGroup(current.getTournamentId(), stageId, groupId);
        TournamentTeam home = loadTournamentTeam(homeTournamentTeamId);
        TournamentTeam away = loadTournamentTeam(awayTournamentTeamId);
        validateTeamsBelongToTournament(current.getTournamentId(), home, away);
        tournamentStageProgressionService.assertMatchStageCanBeManaged(
                tournament,
                stageId,
                groupId,
                homeTournamentTeamId,
                awayTournamentTeamId
        );
        validateKnockoutCross(stage, current.getId(), current.getTournamentId(), groupId, roundNumber, matchdayNumber, homeTournamentTeamId, awayTournamentTeamId);
        validateDuplicateMatch(
                current.getId(),
                current.getTournamentId(),
                stageId,
                groupId,
                roundNumber,
                matchdayNumber,
                homeTournamentTeamId,
                awayTournamentTeamId
        );
        validateScoresAndWinner(stage, homeTournamentTeamId, awayTournamentTeamId, status, homeScore, awayScore, winnerTournamentTeamId);
    }

    private void validateDuplicateMatch(
            Long currentId,
            Long tournamentId,
            Long stageId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    ) {
        boolean exists = currentId == null
                ? matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamId(
                        tournamentId,
                        stageId,
                        groupId,
                        roundNumber,
                        matchdayNumber,
                        homeTournamentTeamId,
                        awayTournamentTeamId
                )
                : matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamIdAndIdNot(
                        tournamentId,
                        stageId,
                        groupId,
                        roundNumber,
                        matchdayNumber,
                        homeTournamentTeamId,
                        awayTournamentTeamId,
                        currentId
                );

        if (exists) {
            throw new BusinessException("Ya existe un partido con el mismo cruce, roundNumber y matchdayNumber en ese alcance");
        }
    }

    private Tournament loadTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException("El tournamentId enviado no existe"));
    }

    private TournamentStage validateStageAndGroup(Long tournamentId, Long stageId, Long groupId) {
        TournamentStage stage = null;
        if (stageId != null) {
            stage = tournamentStageRepository.findById(stageId)
                    .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));

            if (!stage.getTournamentId().equals(tournamentId)) {
                throw new BusinessException("El stageId no pertenece al torneo indicado");
            }
        }

        if (groupId != null) {
            if (stageId == null) {
                throw new BusinessException("groupId requiere un stageId");
            }
            StageGroup group = stageGroupRepository.findById(groupId)
                    .orElseThrow(() -> new BusinessException("El groupId enviado no existe"));

            if (!group.getStageId().equals(stageId)) {
                throw new BusinessException("El groupId no pertenece al stageId indicado");
            }
        }
        return stage;
    }

    private TournamentTeam loadTournamentTeam(Long tournamentTeamId) {
        return tournamentTeamRepository.findById(tournamentTeamId)
                .orElseThrow(() -> new BusinessException("El tournamentTeamId enviado no existe: " + tournamentTeamId));
    }

    private void validateTeamsBelongToTournament(Long tournamentId, TournamentTeam home, TournamentTeam away) {
        if (home.getId().equals(away.getId())) {
            throw new BusinessException("El equipo local y visitante no pueden ser el mismo");
        }

        if (!home.getTournamentId().equals(tournamentId) || !away.getTournamentId().equals(tournamentId)) {
            throw new BusinessException("Los equipos del partido deben pertenecer al torneo indicado");
        }
    }

    private void validateScoresAndWinner(
            TournamentStage stage,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            MatchGameStatus status,
            Integer homeScore,
            Integer awayScore,
            Long winnerTournamentTeamId
    ) {
        boolean bothScoresNull = homeScore == null && awayScore == null;
        boolean bothScoresPresent = homeScore != null && awayScore != null;

        if (!bothScoresNull && !bothScoresPresent) {
            throw new BusinessException("homeScore y awayScore deben enviarse ambos o ninguno");
        }

        if (homeScore != null && (homeScore < 0 || awayScore < 0)) {
            throw new BusinessException("Los scores no pueden ser negativos");
        }

        if (status == MatchGameStatus.PLAYED || status == MatchGameStatus.FORFEIT) {
            if (!bothScoresPresent) {
                throw new BusinessException("Un partido PLAYED o FORFEIT requiere scores");
            }
        }

        if (winnerTournamentTeamId != null
                && !winnerTournamentTeamId.equals(homeTournamentTeamId)
                && !winnerTournamentTeamId.equals(awayTournamentTeamId)) {
            throw new BusinessException("winnerTournamentTeamId debe ser uno de los participantes");
        }

        if (status == MatchGameStatus.SCHEDULED && (homeScore != null || winnerTournamentTeamId != null)) {
            throw new BusinessException("Un partido SCHEDULED no debe tener scores ni ganador");
        }

        if (status == MatchGameStatus.PLAYED) {
            validateWinnerConsistency(homeTournamentTeamId, awayTournamentTeamId, homeScore, awayScore, winnerTournamentTeamId);
        }

        if (status == MatchGameStatus.FORFEIT) {
            if (winnerTournamentTeamId == null) {
                throw new BusinessException("Un partido FORFEIT requiere winnerTournamentTeamId");
            }
            validateWinnerConsistency(homeTournamentTeamId, awayTournamentTeamId, homeScore, awayScore, winnerTournamentTeamId);
        }

        if (status == MatchGameStatus.CANCELLED && (homeScore != null || winnerTournamentTeamId != null)) {
            throw new BusinessException("Un partido CANCELLED no debe tener scores ni ganador");
        }

        if (stage != null && stage.getStageType() == TournamentStageType.KNOCKOUT) {
            validateKnockoutResultRules(status, homeScore, awayScore, winnerTournamentTeamId);
        }
    }

    private void validateKnockoutResultRules(
            MatchGameStatus status,
            Integer homeScore,
            Integer awayScore,
            Long winnerTournamentTeamId
    ) {
        if (status == MatchGameStatus.PLAYED) {
            if (homeScore == null || awayScore == null) {
                return;
            }
            if (homeScore.equals(awayScore)) {
                throw new BusinessException("Un partido KNOCKOUT no puede terminar empatado");
            }
            if (winnerTournamentTeamId == null) {
                throw new BusinessException("Un partido KNOCKOUT cerrado requiere winnerTournamentTeamId");
            }
        }
    }

    private void validateKnockoutCross(
            TournamentStage stage,
            Long currentId,
            Long tournamentId,
            Long groupId,
            Integer roundNumber,
            Integer matchdayNumber,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    ) {
        if (stage == null || stage.getStageType() != TournamentStageType.KNOCKOUT) {
            return;
        }

        if (roundNumber == null || matchdayNumber == null) {
            throw new BusinessException("Un partido KNOCKOUT requiere roundNumber y matchdayNumber");
        }

        boolean reverseExists = currentId == null
                ? matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamId(
                        tournamentId,
                        stage.getId(),
                        groupId,
                        roundNumber,
                        matchdayNumber,
                        awayTournamentTeamId,
                        homeTournamentTeamId
                )
                : matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndRoundNumberAndMatchdayNumberAndHomeTournamentTeamIdAndAwayTournamentTeamIdAndIdNot(
                        tournamentId,
                        stage.getId(),
                        groupId,
                        roundNumber,
                        matchdayNumber,
                        awayTournamentTeamId,
                        homeTournamentTeamId,
                        currentId
                );

        if (reverseExists) {
            throw new BusinessException("No se permite duplicar un cruce KNOCKOUT invirtiendo local y visitante en el mismo roundNumber y matchdayNumber");
        }
    }

    private void validateWinnerConsistency(
            Long homeTournamentTeamId,
            Long awayTournamentTeamId,
            Integer homeScore,
            Integer awayScore,
            Long winnerTournamentTeamId
    ) {
        if (homeScore == null || awayScore == null) {
            return;
        }

        if (homeScore.equals(awayScore)) {
            if (winnerTournamentTeamId != null) {
                throw new BusinessException("Un empate no debe registrar winnerTournamentTeamId");
            }
            return;
        }

        if (winnerTournamentTeamId == null) {
            return;
        }

        Long expectedWinner = homeScore > awayScore ? homeTournamentTeamId : awayTournamentTeamId;
        if (!winnerTournamentTeamId.equals(expectedWinner)) {
            throw new BusinessException("winnerTournamentTeamId no coincide con el marcador enviado");
        }
    }
}
