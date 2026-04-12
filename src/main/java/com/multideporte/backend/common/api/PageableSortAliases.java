package com.multideporte.backend.common.api;

import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableSortAliases {

    private PageableSortAliases() {
    }

    public static Pageable map(Pageable pageable, Map<String, String> aliases) {
        if (pageable.isUnpaged() || pageable.getSort().isUnsorted() || aliases.isEmpty()) {
            return pageable;
        }

        Sort mappedSort = Sort.by(pageable.getSort().stream()
                .map(order -> order.withProperty(aliases.getOrDefault(order.getProperty(), order.getProperty())))
                .toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }
}
