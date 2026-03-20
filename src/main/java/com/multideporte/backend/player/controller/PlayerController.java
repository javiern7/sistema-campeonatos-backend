package com.multideporte.backend.player.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.player.dto.request.PlayerCreateRequest;
import com.multideporte.backend.player.dto.request.PlayerUpdateRequest;
import com.multideporte.backend.player.dto.response.PlayerResponse;
import com.multideporte.backend.player.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<PlayerResponse>> create(@Valid @RequestBody PlayerCreateRequest request) {
        PlayerResponse response = playerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PLAYER_CREATED", "Jugador creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlayerResponse>> getById(@PathVariable Long id) {
        PlayerResponse response = playerService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("PLAYER_FOUND", "Jugador obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlayerResponse>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<PlayerResponse> response = playerService.getAll(search, documentType, documentNumber, active, pageable);
        return ResponseEntity.ok(ApiResponse.success("PLAYER_PAGE", "Jugadores obtenidos correctamente", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<PlayerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PlayerUpdateRequest request
    ) {
        PlayerResponse response = playerService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("PLAYER_UPDATED", "Jugador actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        playerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("PLAYER_DELETED", "Jugador eliminado correctamente"));
    }
}
