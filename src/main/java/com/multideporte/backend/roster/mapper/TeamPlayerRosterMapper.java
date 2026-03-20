package com.multideporte.backend.roster.mapper;

import com.multideporte.backend.roster.dto.request.TeamPlayerRosterCreateRequest;
import com.multideporte.backend.roster.dto.request.TeamPlayerRosterUpdateRequest;
import com.multideporte.backend.roster.dto.response.TeamPlayerRosterResponse;
import com.multideporte.backend.roster.entity.TeamPlayerRoster;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TeamPlayerRosterMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TeamPlayerRoster toEntity(TeamPlayerRosterCreateRequest request);

    TeamPlayerRosterResponse toResponse(TeamPlayerRoster entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournamentTeamId", ignore = true)
    @Mapping(target = "playerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget TeamPlayerRoster entity, TeamPlayerRosterUpdateRequest request);
}
