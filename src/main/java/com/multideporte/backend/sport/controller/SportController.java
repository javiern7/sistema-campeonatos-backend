package com.multideporte.backend.sport.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.sport.dto.request.SportCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionUpdateRequest;
import com.multideporte.backend.sport.dto.request.SportUpdateRequest;
import com.multideporte.backend.sport.dto.response.CompetitionFormatResponse;
import com.multideporte.backend.sport.dto.response.SportPositionResponse;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.service.SportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    @PostMapping
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<SportResponse>> create(@Valid @RequestBody SportCreateRequest request) {
        SportResponse response = sportService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SPORT_CREATED", "Deporte creado correctamente", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.SPORTS_READ + "')")
    public ResponseEntity<ApiResponse<SportResponse>> getById(@PathVariable Long id) {
        SportResponse response = sportService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("SPORT_FOUND", "Deporte obtenido correctamente", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.SPORTS_READ + "')")
    public ResponseEntity<ApiResponse<List<SportResponse>>> getAll(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<SportResponse> response = sportService.getAll(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("SPORT_LIST", "Sports obtenidos correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<SportResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SportUpdateRequest request
    ) {
        SportResponse response = sportService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("SPORT_UPDATED", "Deporte actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sportService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("SPORT_DELETED", "Deporte eliminado correctamente"));
    }

    @GetMapping("/{sportId}/positions")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.SPORTS_READ + "')")
    public ResponseEntity<ApiResponse<List<SportPositionResponse>>> getPositions(
            @PathVariable Long sportId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<SportPositionResponse> response = sportService.getPositions(sportId, activeOnly);
        return ResponseEntity.ok(ApiResponse.success("SPORT_POSITION_LIST", "Posiciones obtenidas correctamente", response));
    }

    @PostMapping("/{sportId}/positions")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<SportPositionResponse>> createPosition(
            @PathVariable Long sportId,
            @Valid @RequestBody SportPositionCreateRequest request
    ) {
        SportPositionResponse response = sportService.createPosition(sportId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("SPORT_POSITION_CREATED", "Posicion creada correctamente", response));
    }

    @PutMapping("/{sportId}/positions/{positionId}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<SportPositionResponse>> updatePosition(
            @PathVariable Long sportId,
            @PathVariable Long positionId,
            @Valid @RequestBody SportPositionUpdateRequest request
    ) {
        SportPositionResponse response = sportService.updatePosition(sportId, positionId, request);
        return ResponseEntity.ok(ApiResponse.success("SPORT_POSITION_UPDATED", "Posicion actualizada correctamente", response));
    }

    @DeleteMapping("/{sportId}/positions/{positionId}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_BASIC_CONFIGURATION)
    public ResponseEntity<ApiResponse<Void>> deletePosition(
            @PathVariable Long sportId,
            @PathVariable Long positionId
    ) {
        sportService.deletePosition(sportId, positionId);
        return ResponseEntity.ok(ApiResponse.success("SPORT_POSITION_DELETED", "Posicion eliminada correctamente"));
    }

    @GetMapping("/competition-formats")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.SPORTS_READ + "')")
    public ResponseEntity<ApiResponse<List<CompetitionFormatResponse>>> getCompetitionFormats() {
        List<CompetitionFormatResponse> response = sportService.getCompetitionFormats();
        return ResponseEntity.ok(ApiResponse.success("COMPETITION_FORMAT_LIST", "Formatos obtenidos correctamente", response));
    }
}
