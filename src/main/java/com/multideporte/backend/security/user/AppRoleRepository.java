package com.multideporte.backend.security.user;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    @EntityGraph(attributePaths = "permissions")
    List<AppRole> findAllByOrderByCodeAsc();
}
