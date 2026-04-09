package com.multideporte.backend.finance.validation;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import com.multideporte.backend.tournament.entity.Tournament;
import com.multideporte.backend.tournament.repository.TournamentRepository;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import com.multideporte.backend.tournamentteam.repository.TournamentTeamRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BasicFinanceValidator {

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;

    public Tournament requireTournament(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado con id: " + tournamentId));
    }

    public TournamentTeam requireTournamentTeamInTournament(Long tournamentId, Long tournamentTeamId) {
        TournamentTeam tournamentTeam = tournamentTeamRepository.findById(tournamentTeamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo de torneo no encontrado con id: " + tournamentTeamId));
        if (!Objects.equals(tournamentTeam.getTournamentId(), tournamentId)) {
            throw new BusinessException("El tournamentTeamId enviado no pertenece al torneo indicado");
        }
        return tournamentTeam;
    }

    public void validateMovement(
            Long tournamentId,
            Long tournamentTeamId,
            FinancialMovementType movementType,
            FinancialMovementCategory category
    ) {
        requireTournament(tournamentId);

        if (category.movementType() != movementType) {
            throw new BusinessException("La categoria financiera no corresponde al tipo de movimiento indicado");
        }

        if (movementType == FinancialMovementType.EXPENSE && tournamentTeamId != null) {
            throw new BusinessException("Los gastos de esta etapa se registran solo a nivel de torneo");
        }

        if (tournamentTeamId != null) {
            requireTournamentTeamInTournament(tournamentId, tournamentTeamId);
        }
    }
}
