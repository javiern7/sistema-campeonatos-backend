package com.multideporte.backend.sport.repository;

import com.multideporte.backend.sport.entity.SportPosition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportPositionRepository extends JpaRepository<SportPosition, Long> {

    List<SportPosition> findBySportIdOrderByDisplayOrderAscNameAsc(Long sportId);

    List<SportPosition> findBySportIdAndActiveOrderByDisplayOrderAscNameAsc(Long sportId, Boolean active);

    Optional<SportPosition> findBySportIdAndCodeIgnoreCase(Long sportId, String code);

    Optional<SportPosition> findBySportIdAndDisplayOrder(Long sportId, Integer displayOrder);

    boolean existsBySportId(Long sportId);
}
