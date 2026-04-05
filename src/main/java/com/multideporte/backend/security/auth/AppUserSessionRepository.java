package com.multideporte.backend.security.auth;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserSessionRepository extends JpaRepository<AppUserSession, Long> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<AppUserSession> findByAccessTokenHash(String accessTokenHash);

    @EntityGraph(attributePaths = {"user", "user.roles"})
    @Query("""
            select distinct session
            from AppUserSession session
            join fetch session.user user
            join fetch user.roles roles
            where session.accessTokenHash = :accessTokenHash
              and session.revokedAt is null
              and session.accessTokenExpiresAt > :now
            """)
    Optional<AppUserSession> findActiveByAccessTokenHash(
            @Param("accessTokenHash") String accessTokenHash,
            @Param("now") OffsetDateTime now
    );

    @EntityGraph(attributePaths = {"user", "user.roles"})
    @Query("""
            select distinct session
            from AppUserSession session
            join fetch session.user user
            join fetch user.roles roles
            where session.refreshTokenHash = :refreshTokenHash
              and session.revokedAt is null
              and session.refreshTokenExpiresAt > :now
            """)
    Optional<AppUserSession> findActiveByRefreshTokenHash(
            @Param("refreshTokenHash") String refreshTokenHash,
            @Param("now") OffsetDateTime now
    );
}
