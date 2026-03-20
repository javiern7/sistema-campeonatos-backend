package com.multideporte.backend.sport.mapper;

import com.multideporte.backend.sport.dto.response.SportResponse;
import com.multideporte.backend.sport.entity.Sport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SportMapper {

    SportResponse toResponse(Sport entity);
}
