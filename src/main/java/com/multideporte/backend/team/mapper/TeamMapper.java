package com.multideporte.backend.team.mapper;

import com.multideporte.backend.team.dto.request.TeamCreateRequest;
import com.multideporte.backend.team.dto.request.TeamUpdateRequest;
import com.multideporte.backend.team.dto.response.TeamResponse;
import com.multideporte.backend.team.entity.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Team toEntity(TeamCreateRequest request);

    TeamResponse toResponse(Team entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Team entity, TeamUpdateRequest request);
}
