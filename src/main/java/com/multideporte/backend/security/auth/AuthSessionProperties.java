package com.multideporte.backend.security.auth;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.session")
@Getter
@Setter
public class AuthSessionProperties {

    private Duration accessTokenTtl = Duration.ofHours(8);
    private Duration refreshTokenTtl = Duration.ofDays(7);
}
