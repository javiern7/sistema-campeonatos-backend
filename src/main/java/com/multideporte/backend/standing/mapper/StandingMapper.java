package com.multideporte.backend.standing.mapper;

import com.multideporte.backend.standing.dto.request.StandingCreateRequest;
import com.multideporte.backend.standing.dto.request.StandingUpdateRequest;
import com.multideporte.backend.standing.dto.response.StandingResponse;
import com.multideporte.backend.standing.entity.Standing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StandingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Standing toEntity(StandingCreateRequest request);

    StandingResponse toResponse(Standing entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Standing entity, StandingUpdateRequest request);
}
