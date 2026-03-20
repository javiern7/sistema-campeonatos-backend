package com.multideporte.backend.stagegroup.service;

import com.multideporte.backend.stagegroup.dto.request.StageGroupCreateRequest;
import com.multideporte.backend.stagegroup.dto.request.StageGroupUpdateRequest;
import com.multideporte.backend.stagegroup.dto.response.StageGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StageGroupService {

    StageGroupResponse create(StageGroupCreateRequest request);

    StageGroupResponse getById(Long id);

    Page<StageGroupResponse> getAll(Long stageId, String code, Pageable pageable);

    StageGroupResponse update(Long id, StageGroupUpdateRequest request);

    void delete(Long id);
}
