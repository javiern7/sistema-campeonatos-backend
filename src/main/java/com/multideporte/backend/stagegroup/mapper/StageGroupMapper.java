package com.multideporte.backend.stagegroup.mapper;

import com.multideporte.backend.stagegroup.dto.request.StageGroupCreateRequest;
import com.multideporte.backend.stagegroup.dto.request.StageGroupUpdateRequest;
import com.multideporte.backend.stagegroup.dto.response.StageGroupResponse;
import com.multideporte.backend.stagegroup.entity.StageGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StageGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    StageGroup toEntity(StageGroupCreateRequest request);

    StageGroupResponse toResponse(StageGroup entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget StageGroup entity, StageGroupUpdateRequest request);
}
