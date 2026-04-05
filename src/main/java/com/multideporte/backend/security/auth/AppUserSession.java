package com.multideporte.backend.security.auth;

import com.multideporte.backend.security.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user_session")
@Getter
@Setter
public class AppUserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "authentication_scheme", nullable = false)
    private String authenticationScheme;

    @Column(name = "access_token_hash", nullable = false)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    @Column(name = "access_token_expires_at", nullable = false)
    private OffsetDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private OffsetDateTime refreshTokenExpiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
