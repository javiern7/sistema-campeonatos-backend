package com.multideporte.backend.tournamentteam.mapper;

import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamCreateRequest;
import com.multideporte.backend.tournamentteam.dto.request.TournamentTeamUpdateRequest;
import com.multideporte.backend.tournamentteam.dto.response.TournamentTeamResponse;
import com.multideporte.backend.tournamentteam.entity.TournamentTeam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TournamentTeamMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    TournamentTeam toEntity(TournamentTeamCreateRequest request);

    TournamentTeamResponse toResponse(TournamentTeam entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    void updateEntity(@MappingTarget TournamentTeam entity, TournamentTeamUpdateRequest request);
}
