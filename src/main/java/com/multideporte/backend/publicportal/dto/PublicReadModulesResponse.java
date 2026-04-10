package com.multideporte.backend.publicportal.dto;

public record PublicReadModulesResponse(
        boolean tournamentsEnabled,
        boolean standingsEnabled,
        boolean resultsEnabled,
        boolean approvedPiecesEnabled
) {
}
