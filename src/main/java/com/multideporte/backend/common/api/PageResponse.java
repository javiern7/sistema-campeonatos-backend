package com.multideporte.backend.common.api;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<SortOrderResponse> sort
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        List<SortOrderResponse> sort = page.getSort().stream()
                .map(order -> new SortOrderResponse(order.getProperty(), order.getDirection().name(), order.isIgnoreCase()))
                .toList();

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                sort
        );
    }

    public record SortOrderResponse(
            String property,
            String direction,
            boolean ignoreCase
    ) {
        public static SortOrderResponse from(Sort.Order order) {
            return new SortOrderResponse(order.getProperty(), order.getDirection().name(), order.isIgnoreCase());
        }
    }
}
