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
import com.multideporte.backend.standing.entity.Standing;
import com.multideporte.backend.standing.repository.StandingRepository;
import com.multideporte.backend.tournament.dto.response.TournamentKnockoutProgressionResponse;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.entity.TournamentFormat;
import com.multideporte.backend.tournament.entity.TournamentStatus;
import com.multideporte.backend.tournament.service.TournamentStageProgressionService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentStageProgressionServiceImpl implements TournamentStageProgressionService {

    private final TournamentStageRepository tournamentStageRepository;
    private final StageGroupRepository stageGroupRepository;
    private final MatchGameRepository matchGameRepository;
    private final StandingRepository standingRepository;

    @Override
    @Transactional
    public TournamentKnockoutProgressionResponse progressGroupsThenKnockout(Tournament tournament) {
        validateTournamentForProgression(tournament);

        TournamentStage sourceStage = resolveActiveGroupStage(tournament.getId());
        TournamentStage targetStage = resolveNextKnockoutStage(tournament.getId(), sourceStage.getSequenceOrder());
        List<Long> qualifiedTeams = resolveQualifiedTeams(sourceStage, tournament.getId());

        if (matchGameRepository.existsByStageId(targetStage.getId())) {
            throw new BusinessException("La etapa KNOCKOUT destino no debe tener partidos cargados antes de la progresion");
        }

        deactivateAllStages(tournament.getId());
        sourceStage.setActive(false);
        targetStage.setActive(true);
        tournamentStageRepository.save(sourceStage);
        tournamentStageRepository.save(targetStage);

        return new TournamentKnockoutProgressionResponse(
                tournament.getId(),
                sourceStage.getId(),
                targetStage.getId(),
                qualifiedTeams.size(),
                qualifiedTeams
        );
    }

    @Override
    public void assertMatchStageCanBeManaged(
            Tournament tournament,
            Long stageId,
            Long groupId,
            Long homeTournamentTeamId,
            Long awayTournamentTeamId
    ) {
        if (stageId == null) {
            if (tournament.getFormat() == TournamentFormat.GROUPS_THEN_KNOCKOUT) {
                throw new BusinessException("Los partidos de un torneo GROUPS_THEN_KNOCKOUT requieren stageId");
            }
            return;
        }

        TournamentStage stage = tournamentStageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));

        if (!Boolean.TRUE.equals(stage.getActive())) {
            throw new BusinessException("Solo se permite gestionar partidos en la etapa activa del torneo");
        }

        if (stage.getStageType() == TournamentStageType.GROUP_STAGE && groupId == null) {
            throw new BusinessException("Los partidos de una etapa GROUP_STAGE requieren groupId");
        }

        if (stage.getStageType() == TournamentStageType.KNOCKOUT && groupId != null) {
            throw new BusinessException("Los partidos de una etapa KNOCKOUT no deben asociarse a groupId");
        }

        if (tournament.getFormat() == TournamentFormat.GROUPS_THEN_KNOCKOUT
                && tournament.getStatus() == TournamentStatus.IN_PROGRESS
                && stage.getStageType() == TournamentStageType.KNOCKOUT) {
            List<Long> qualifiedTeams = resolveQualifiedTeamsFromPreviousGroupStage(tournament.getId(), stage.getSequenceOrder());
            if (!qualifiedTeams.contains(homeTournamentTeamId) || !qualifiedTeams.contains(awayTournamentTeamId)) {
                throw new BusinessException("Los partidos KNOCKOUT solo pueden involucrar equipos clasificados desde grupos");
            }
        }
    }

    @Override
    public void assertStandingsCanBeRecalculated(Tournament tournament, Long stageId, Long groupId) {
        if (stageId == null) {
            if (tournament.getFormat() == TournamentFormat.GROUPS_THEN_KNOCKOUT) {
                throw new BusinessException("Los standings de un torneo GROUPS_THEN_KNOCKOUT requieren stageId y groupId de la etapa activa");
            }
            return;
        }

        TournamentStage stage = tournamentStageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException("El stageId enviado no existe"));

        if (!Boolean.TRUE.equals(stage.getActive())) {
            throw new BusinessException("Solo se permite recalcular standings para la etapa activa del torneo");
        }

        if (stage.getStageType() == TournamentStageType.KNOCKOUT) {
            throw new BusinessException("No se permite recalcular standings para una etapa KNOCKOUT");
        }

        if (stage.getStageType() == TournamentStageType.GROUP_STAGE && groupId == null) {
            throw new BusinessException("Los standings de una etapa GROUP_STAGE requieren groupId");
        }
    }

    private void validateTournamentForProgression(Tournament tournament) {
        if (tournament.getFormat() != TournamentFormat.GROUPS_THEN_KNOCKOUT) {
            throw new BusinessException("Solo los torneos GROUPS_THEN_KNOCKOUT pueden progresar a KNOCKOUT");
        }
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("Solo un torneo IN_PROGRESS puede progresar a KNOCKOUT");
        }
    }

    private TournamentStage resolveActiveGroupStage(Long tournamentId) {
        List<TournamentStage> activeStages = tournamentStageRepository.findAllByTournamentIdAndActiveTrueOrderBySequenceOrderAsc(tournamentId);
        if (activeStages.size() != 1) {
            throw new BusinessException("La progresion requiere exactamente una etapa activa");
        }

        TournamentStage sourceStage = activeStages.get(0);
        if (sourceStage.getStageType() != TournamentStageType.GROUP_STAGE) {
            throw new BusinessException("La progresion a KNOCKOUT requiere una etapa GROUP_STAGE activa");
        }
        return sourceStage;
    }

    private TournamentStage resolveNextKnockoutStage(Long tournamentId, Integer sourceSequenceOrder) {
        return tournamentStageRepository.findAllByTournamentIdAndStageTypeOrderBySequenceOrderAsc(
                        tournamentId,
                        TournamentStageType.KNOCKOUT
                ).stream()
                .filter(stage -> stage.getSequenceOrder() > sourceSequenceOrder)
                .findFirst()
                .orElseThrow(() -> new BusinessException("No existe una etapa KNOCKOUT posterior preparada para progresion"));
    }

    private List<Long> resolveQualifiedTeams(TournamentStage sourceStage, Long tournamentId) {
        List<StageGroup> groups = stageGroupRepository.findAllByStageIdOrderBySequenceOrderAsc(sourceStage.getId());
        if (groups.size() < 2) {
            throw new BusinessException("La progresion a KNOCKOUT requiere al menos 2 grupos en la etapa activa");
        }

        List<Long> qualifiedTeams = new ArrayList<>();
        Set<Long> uniqueQualifiedTeams = new HashSet<>();

        for (StageGroup group : groups) {
            if (matchGameRepository.existsByTournamentIdAndStageIdAndGroupIdAndStatus(
                    tournamentId,
                    sourceStage.getId(),
                    group.getId(),
                    MatchGameStatus.SCHEDULED
            )) {
                throw new BusinessException("No se puede progresar a KNOCKOUT con partidos de grupos pendientes");
            }

            List<MatchGame> completedMatches = matchGameRepository.findAllByTournamentIdAndStageIdAndGroupIdAndStatusIn(
                    tournamentId,
                    sourceStage.getId(),
                    group.getId(),
                    EnumSet.of(MatchGameStatus.PLAYED, MatchGameStatus.FORFEIT)
            );
            if (completedMatches.isEmpty()) {
                throw new BusinessException("Cada grupo debe tener partidos cerrados antes de progresar a KNOCKOUT");
            }

            List<Standing> standings = standingRepository.findAllByTournamentIdAndStageIdAndGroupIdOrderByRankPositionAsc(
                    tournamentId,
                    sourceStage.getId(),
                    group.getId()
            );
            if (standings.isEmpty()) {
                throw new BusinessException("No se puede progresar a KNOCKOUT sin standings recalculados por grupo");
            }
            if (standings.get(0).getRankPosition() == null) {
                throw new BusinessException("Los standings del grupo deben tener posiciones de ranking antes de progresar");
            }

            Long qualifiedTeamId = standings.get(0).getTournamentTeamId();
            if (!uniqueQualifiedTeams.add(qualifiedTeamId)) {
                throw new BusinessException("Un mismo equipo no puede clasificar dos veces a la fase KNOCKOUT");
            }
            qualifiedTeams.add(qualifiedTeamId);
        }

        if (!isPowerOfTwo(qualifiedTeams.size())) {
            throw new BusinessException("La clasificacion a KNOCKOUT requiere una cantidad de equipos clasificados potencia de 2");
        }

        return qualifiedTeams;
    }

    private List<Long> resolveQualifiedTeamsFromPreviousGroupStage(Long tournamentId, Integer knockoutSequenceOrder) {
        TournamentStage previousGroupStage = tournamentStageRepository.findAllByTournamentIdAndStageTypeOrderBySequenceOrderAsc(
                        tournamentId,
                        TournamentStageType.GROUP_STAGE
                ).stream()
                .filter(stage -> stage.getSequenceOrder() < knockoutSequenceOrder)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new BusinessException("No existe una etapa GROUP_STAGE previa para validar clasificados"));

        return resolveQualifiedTeams(previousGroupStage, tournamentId);
    }

    private void deactivateAllStages(Long tournamentId) {
        List<TournamentStage> stages = tournamentStageRepository.findAllByTournamentIdOrderBySequenceOrderAsc(tournamentId);
        for (TournamentStage stage : stages) {
            stage.setActive(false);
        }
        tournamentStageRepository.saveAll(stages);
    }

    private boolean isPowerOfTwo(int value) {
        return value >= 2 && (value & (value - 1)) == 0;
    }
}
