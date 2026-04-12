package com.multideporte.backend.matchevent.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.matchevent.dto.request.MatchEventAnnulRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventCreateRequest;
import com.multideporte.backend.matchevent.dto.request.MatchEventUpdateRequest;
import com.multideporte.backend.matchevent.dto.response.MatchEventResponse;
import com.multideporte.backend.matchevent.service.MatchEventService;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.security.auth.SecurityPermissions;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MatchEventController {

    private final MatchEventService matchEventService;
    private final OperationalAuditService operationalAuditService;

    @GetMapping("/matches/{matchId}/events")
    @PreAuthorize("hasAuthority('" + SecurityPermissions.MATCHES_READ + "')")
    public ResponseEntity<ApiResponse<List<MatchEventResponse>>> getMatchEvents(@PathVariable Long matchId) {
        List<MatchEventResponse> response = matchEventService.getMatchEvents(matchId);
        return ResponseEntity.ok(ApiResponse.success(
                "MATCH_EVENTS_FOUND",
                "Eventos del partido obtenidos correctamente",
                response
        ));
    }

    @PostMapping("/matches/{matchId}/events")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<MatchEventResponse>> create(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchEventCreateRequest request
    ) {
        MatchEventResponse response = matchEventService.create(matchId, request);
        operationalAuditService.auditSuccess("MATCH_EVENT_CREATE", "MATCH", matchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "MATCH_EVENT_CREATED",
                        "Evento de partido creado correctamente",
                        response
                ));
    }

    @PutMapping("/matches/{matchId}/events/{eventId}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<MatchEventResponse>> update(
            @PathVariable Long matchId,
            @PathVariable Long eventId,
            @Valid @RequestBody MatchEventUpdateRequest request
    ) {
        MatchEventResponse response = matchEventService.update(matchId, eventId, request);
        operationalAuditService.auditSuccess("MATCH_EVENT_UPDATE", "MATCH_EVENT", eventId);
        return ResponseEntity.ok(ApiResponse.success(
                "MATCH_EVENT_UPDATED",
                "Evento de partido actualizado correctamente",
                response
        ));
    }

    @DeleteMapping("/matches/{matchId}/events/{eventId}")
    @PreAuthorize(SecurityPermissions.CAN_MANAGE_MATCHES)
    public ResponseEntity<ApiResponse<MatchEventResponse>> annul(
            @PathVariable Long matchId,
            @PathVariable Long eventId,
            @Valid @RequestBody(required = false) MatchEventAnnulRequest request
    ) {
        MatchEventResponse response = matchEventService.annul(matchId, eventId, request);
        operationalAuditService.auditSuccess("MATCH_EVENT_ANNUL", "MATCH_EVENT", eventId);
        return ResponseEntity.ok(ApiResponse.success(
                "MATCH_EVENT_ANNULLED",
                "Evento de partido anulado correctamente",
                response
        ));
    }
}
