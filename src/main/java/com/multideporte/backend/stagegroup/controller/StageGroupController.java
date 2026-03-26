package com.multideporte.backend.stagegroup.controller;

import com.multideporte.backend.common.api.ApiResponse;
import com.multideporte.backend.common.api.PageResponse;
import com.multideporte.backend.stagegroup.dto.request.StageGroupCreateRequest;
import com.multideporte.backend.stagegroup.dto.request.StageGroupUpdateRequest;
import com.multideporte.backend.stagegroup.dto.response.StageGroupResponse;
import com.multideporte.backend.stagegroup.service.StageGroupService;
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
@RequestMapping("/stage-groups")
@RequiredArgsConstructor
public class StageGroupController {

    private final StageGroupService stageGroupService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<StageGroupResponse>> create(@Valid @RequestBody StageGroupCreateRequest request) {
        StageGroupResponse response = stageGroupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("STAGE_GROUP_CREATED", "Grupo creado correctamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StageGroupResponse>> getById(@PathVariable Long id) {
        StageGroupResponse response = stageGroupService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("STAGE_GROUP_FOUND", "Grupo obtenido correctamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StageGroupResponse>>> getAll(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String code,
            @PageableDefault(size = 20, sort = "sequenceOrder") Pageable pageable
    ) {
        Page<StageGroupResponse> response = stageGroupService.getAll(stageId, code, pageable);
        return ResponseEntity.ok(ApiResponse.success("STAGE_GROUP_PAGE", "Grupos obtenidos correctamente", PageResponse.from(response)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TOURNAMENT_ADMIN')")
    public ResponseEntity<ApiResponse<StageGroupResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody StageGroupUpdateRequest request
    ) {
        StageGroupResponse response = stageGroupService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("STAGE_GROUP_UPDATED", "Grupo actualizado correctamente", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        stageGroupService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("STAGE_GROUP_DELETED", "Grupo eliminado correctamente"));
    }
}
