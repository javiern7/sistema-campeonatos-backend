package com.multideporte.backend.player.mapper;

import com.multideporte.backend.player.dto.request.PlayerCreateRequest;
import com.multideporte.backend.player.dto.request.PlayerUpdateRequest;
import com.multideporte.backend.player.dto.response.PlayerResponse;
import com.multideporte.backend.player.entity.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Player toEntity(PlayerCreateRequest request);

    PlayerResponse toResponse(Player entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Player entity, PlayerUpdateRequest request);
}
