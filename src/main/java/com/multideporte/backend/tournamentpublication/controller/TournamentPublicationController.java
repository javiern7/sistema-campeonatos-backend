package com.multideporte.backend.tournamentpublication.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.audit.OperationalAuditService;
import com.multideporte.backend.tournamentpublication.service.TournamentPublicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/tournaments/{tournamentId}/publication") @RequiredArgsConstructor
public class TournamentPublicationController {
    private final TournamentPublicationService publicationService;
    private final OperationalAuditService auditService;

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('tournaments:publication:manage')")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Long tournamentId) {
        publicationService.publish(tournamentId);
        auditService.auditSuccess("P56_PUBLICATION_PUBLISH", "TOURNAMENT", tournamentId);
        return ResponseEntity.ok(ApiResponse.success("P56_PUBLISHED", "Enlace no listado publicado"));
    }

    @PostMapping("/unpublish")
    @PreAuthorize("hasAuthority('tournaments:publication:manage')")
    public ResponseEntity<ApiResponse<Void>> unpublish(@PathVariable Long tournamentId) {
        publicationService.unpublish(tournamentId);
        auditService.auditSuccess("P56_PUBLICATION_UNPUBLISH", "TOURNAMENT", tournamentId);
        return ResponseEntity.ok(ApiResponse.success("P56_UNPUBLISHED", "Enlace no listado despublicado"));
    }
}
