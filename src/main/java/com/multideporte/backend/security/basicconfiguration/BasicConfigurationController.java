package com.multideporte.backend.security.basicconfiguration;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationResponse;
import com.multideporte.backend.security.basicconfiguration.dto.BasicConfigurationUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operations/basic-configuration")
@RequiredArgsConstructor
public class BasicConfigurationController {

    private final BasicConfigurationService basicConfigurationService;

    @GetMapping
    @PreAuthorize(SecurityPermissions.CAN_READ_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<BasicConfigurationResponse>> getConfiguration() {
        return ResponseEntity.ok(ApiResponse.success(
                "BASIC_CONFIGURATION_FOUND",
                "Configuracion basica obtenida correctamente",
                basicConfigurationService.getConfiguration()
        ));
    }

    @PutMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<BasicConfigurationResponse>> updateConfiguration(
            @Valid @RequestBody BasicConfigurationUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "BASIC_CONFIGURATION_UPDATED",
                "Configuracion basica actualizada correctamente",
                basicConfigurationService.updateConfiguration(request)
        ));
    }
}
