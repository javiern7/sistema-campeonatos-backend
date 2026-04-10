package com.multideporte.backend.security.user;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    Page<AppUser> findAll(Specification<AppUser> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"roles"})
    Optional<AppUser> findDetailedById(Long id);
}
