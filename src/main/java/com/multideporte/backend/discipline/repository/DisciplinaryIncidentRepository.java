package com.multideporte.backend.discipline.repository;

import com.multideporte.backend.discipline.entity.DisciplinaryIncident;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplinaryIncidentRepository extends JpaRepository<DisciplinaryIncident, Long> {

    List<DisciplinaryIncident> findAllByMatchIdOrderByCreatedAtAscIdAsc(Long matchId);

    List<DisciplinaryIncident> findAllByIdIn(Collection<Long> ids);
}
