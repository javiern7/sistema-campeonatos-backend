package com.multideporte.backend.stagegroup.dto.response;

import java.time.OffsetDateTime;

public record StageGroupResponse(
        Long id,
        Long stageId,
        String code,
        String name,
        Integer sequenceOrder,
        OffsetDateTime createdAt
) {
}
