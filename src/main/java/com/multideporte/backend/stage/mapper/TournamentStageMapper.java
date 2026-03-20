package com.multideporte.backend.stage.mapper;

import com.multideporte.backend.stage.dto.request.TournamentStageCreateRequest;
import com.multideporte.backend.stage.dto.request.TournamentStageUpdateRequest;
import com.multideporte.backend.stage.dto.response.TournamentStageResponse;
import com.multideporte.backend.stage.entity.TournamentStage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TournamentStageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TournamentStage toEntity(TournamentStageCreateRequest request);

    TournamentStageResponse toResponse(TournamentStage entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget TournamentStage entity, TournamentStageUpdateRequest request);
}
