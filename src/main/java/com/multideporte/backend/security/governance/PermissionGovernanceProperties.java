package com.multideporte.backend.security.governance;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.permission-governance")
@Getter
@Setter
public class PermissionGovernanceProperties {

    private boolean writeEnabled = false;
    private List<String> mutableRoles = List.of("TOURNAMENT_ADMIN", "OPERATOR");
}
