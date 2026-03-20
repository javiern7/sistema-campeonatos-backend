package com.multideporte.backend.tournament.repository;

import com.multideporte.backend.tournament.entity.Tournament;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {

    boolean existsBySlug(String slug);

    Optional<Tournament> findBySlug(String slug);
}
