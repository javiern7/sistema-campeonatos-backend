package com.multideporte.backend.sport.mapper;

import com.multideporte.backend.sport.dto.request.SportCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionCreateRequest;
import com.multideporte.backend.sport.dto.request.SportPositionUpdateRequest;
import com.multideporte.backend.sport.dto.request.SportUpdateRequest;
import com.multideporte.backend.sport.dto.response.SportPositionResponse;
import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.entity.Sport;
import com.multideporte.backend.sport.entity.SportPosition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Sport toEntity(SportCreateRequest request);

    SportResponse toResponse(Sport entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget Sport entity, SportUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sport", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SportPosition toPositionEntity(SportPositionCreateRequest request);

    @Mapping(target = "sportId", source = "sport.id")
    SportPositionResponse toPositionResponse(SportPosition entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sport", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updatePositionEntity(@MappingTarget SportPosition entity, SportPositionUpdateRequest request);
}
