package com.multideporte.backend.security.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsername(String username);
}
