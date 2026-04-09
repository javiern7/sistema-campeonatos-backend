package com.multideporte.backend.discipline.repository;

import com.multideporte.backend.discipline.entity.DisciplinarySanction;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinarySanctionRepository extends JpaRepository<DisciplinarySanction, Long> {

    boolean existsByIncidentId(Long incidentId);

    boolean existsByIncidentIdAndSanctionType(Long incidentId, DisciplinarySanctionType sanctionType);

    List<DisciplinarySanction> findAllByIncidentIdInOrderByCreatedAtAscIdAsc(Collection<Long> incidentIds);

    List<DisciplinarySanction> findAllByTournamentIdOrderByCreatedAtDescIdDesc(Long tournamentId);
}
