package com.multideporte.backend.sport.dto.response;

public record SportPositionResponse(
        Long id,
        Long sportId,
        String code,
        String name,
        Integer displayOrder,
        Boolean active
) {
}
