package com.multideporte.backend.sport.repository;

import com.multideporte.backend.sport.entity.Sport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, Long> {

    Optional<Sport> findByCodeIgnoreCase(String code);
}
