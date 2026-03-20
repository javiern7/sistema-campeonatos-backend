package com.multideporte.backend.player.repository;

import com.multideporte.backend.player.entity.Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PlayerRepository extends JpaRepository<Player, Long>, JpaSpecificationExecutor<Player> {

    Optional<Player> findByDocumentTypeIgnoreCaseAndDocumentNumberIgnoreCase(String documentType, String documentNumber);
}
