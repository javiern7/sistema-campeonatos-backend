package com.multideporte.backend.security.basicconfiguration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_basic_configuration")
@Getter
@Setter
public class BasicConfiguration {

    @Id
    private Long id;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "support_email", nullable = false)
    private String supportEmail;

    @Column(name = "default_timezone", nullable = false)
    private String defaultTimezone;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
