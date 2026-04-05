package com.multideporte.backend.sport.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.security.auth.SecurityPermissions;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.service.SportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sports")
@RequiredArgsConstructor
public class SportController {

    private final SportService sportService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + SecurityPermissions.SPORTS_READ + "')")
    public ResponseEntity<ApiResponse<List<SportResponse>>> getAll(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<SportResponse> response = sportService.getAll(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("SPORT_LIST", "Sports obtenidos correctamente", response));
    }
}
