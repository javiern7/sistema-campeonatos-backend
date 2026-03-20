package com.multideporte.backend.sport.dto.response;

public record SportResponse(
        Long id,
        String code,
        String name,
        Boolean active
) {
}
