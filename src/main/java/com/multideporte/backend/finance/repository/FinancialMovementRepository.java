package com.multideporte.backend.finance.repository;

import com.multideporte.backend.finance.entity.FinancialMovement;
import com.multideporte.backend.finance.entity.FinancialMovementCategory;
import com.multideporte.backend.finance.entity.FinancialMovementType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, Long> {

    List<FinancialMovement> findAllByTournamentIdOrderByOccurredOnDescIdDesc(Long tournamentId);

    List<FinancialMovement> findAllByTournamentIdAndMovementTypeOrderByOccurredOnDescIdDesc(
            Long tournamentId,
            FinancialMovementType movementType
    );

    List<FinancialMovement> findAllByTournamentIdAndCategoryOrderByOccurredOnDescIdDesc(
            Long tournamentId,
            FinancialMovementCategory category
    );

    List<FinancialMovement> findAllByTournamentIdAndTournamentTeamIdOrderByOccurredOnDescIdDesc(
            Long tournamentId,
            Long tournamentTeamId
    );
}
