package com.multideporte.backend.security.basicconfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationResponse;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationUpdateRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicConfigurationServiceTest {

    @Mock
    private BasicConfigurationRepository basicConfigurationRepository;

    @Mock
    private OperationalAuditService operationalAuditService;

    private BasicConfigurationService basicConfigurationService;

    @BeforeEach
    void setUp() {
        basicConfigurationService = new BasicConfigurationService(
                basicConfigurationRepository,
                operationalAuditService
        );
    }

    @Test
    void shouldUpdateBasicConfigurationWhenValuesChange() {
        BasicConfiguration configuration = configuration(
                "Sistema Campeonatos",
                "operaciones@local.test",
                "America/Lima"
        );
        when(basicConfigurationRepository.findById(1L)).thenReturn(Optional.of(configuration));

        BasicConfigurationResponse response = basicConfigurationService.updateConfiguration(
                new BasicConfigurationUpdateRequest(
                        "Sistema Campeonatos Pro",
                        "soporte@local.test",
                        "America/Bogota"
                )
        );

        assertThat(response.organizationName()).isEqualTo("Sistema Campeonatos Pro");
        assertThat(response.supportEmail()).isEqualTo("soporte@local.test");
        assertThat(response.defaultTimezone()).isEqualTo("America/Bogota");
        verify(basicConfigurationRepository).save(configuration);
        verify(operationalAuditService).auditSuccess(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectUnknownTimezone() {
        when(basicConfigurationRepository.findById(1L)).thenReturn(Optional.of(configuration(
                "Sistema Campeonatos",
                "operaciones@local.test",
                "America/Lima"
        )));

        assertThatThrownBy(() -> basicConfigurationService.updateConfiguration(
                new BasicConfigurationUpdateRequest(
                        "Sistema Campeonatos",
                        "operaciones@local.test",
                        "Zona/Inexistente"
                )
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zona horaria valida");
    }

    private BasicConfiguration configuration(String organizationName, String supportEmail, String defaultTimezone) {
        BasicConfiguration configuration = new BasicConfiguration();
        configuration.setId(1L);
        configuration.setOrganizationName(organizationName);
        configuration.setSupportEmail(supportEmail);
        configuration.setDefaultTimezone(defaultTimezone);
        configuration.setUpdatedAt(OffsetDateTime.parse("2026-04-09T12:00:00Z"));
        return configuration;
    }
}
