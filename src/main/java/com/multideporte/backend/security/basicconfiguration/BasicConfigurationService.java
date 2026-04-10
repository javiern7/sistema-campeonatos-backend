package com.multideporte.backend.security.basicconfiguration;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.common.exception.ResourceNotFoundException;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationResponse;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationUpdateRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasicConfigurationService {

    private static final long CONFIGURATION_ID = 1L;
    private static final String AUDIT_RESOURCE_TYPE = "APP_BASIC_CONFIGURATION";

    private final BasicConfigurationRepository basicConfigurationRepository;
    private final OperationalAuditService operationalAuditService;

    @Transactional(readOnly = true)
    public BasicConfigurationResponse getConfiguration() {
        return toResponse(loadConfiguration());
    }

    @Transactional
    public BasicConfigurationResponse updateConfiguration(BasicConfigurationUpdateRequest request) {
        BasicConfiguration configuration = loadConfiguration();
        validateTimezone(request.defaultTimezone());

        boolean changed = !configuration.getOrganizationName().equals(request.organizationName().trim())
                || !configuration.getSupportEmail().equals(request.supportEmail().trim())
                || !configuration.getDefaultTimezone().equals(request.defaultTimezone().trim());
        if (!changed) {
            throw new BusinessException("La configuracion basica ya contiene exactamente esos valores");
        }

        configuration.setOrganizationName(request.organizationName().trim());
        configuration.setSupportEmail(request.supportEmail().trim());
        configuration.setDefaultTimezone(request.defaultTimezone().trim());
        configuration.setUpdatedAt(OffsetDateTime.now());
        basicConfigurationRepository.save(configuration);

        operationalAuditService.auditSuccess(
                "BASIC_CONFIGURATION_UPDATED",
                AUDIT_RESOURCE_TYPE,
                configuration.getId(),
                null,
                null,
                Map.of(
                        "organizationName", configuration.getOrganizationName(),
                        "supportEmail", configuration.getSupportEmail(),
                        "defaultTimezone", configuration.getDefaultTimezone()
                )
        );

        return toResponse(configuration);
    }

    private BasicConfiguration loadConfiguration() {
        return basicConfigurationRepository.findById(CONFIGURATION_ID)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro la configuracion basica operativa"));
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone.trim());
        } catch (ZoneRulesException | IllegalArgumentException ex) {
            throw new BusinessException("defaultTimezone no corresponde a una zona horaria valida");
        }
    }

    private BasicConfigurationResponse toResponse(BasicConfiguration configuration) {
        return new BasicConfigurationResponse(
                configuration.getOrganizationName(),
                configuration.getSupportEmail(),
                configuration.getDefaultTimezone(),
                configuration.getUpdatedAt()
        );
    }
}
