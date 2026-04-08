package com.multideporte.backend.security.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    @EntityGraph(attributePaths = "permissions")
    List<AppRole> findAllByOrderByCodeAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<AppRole> findByCode(String code);
}
