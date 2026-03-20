package com.multideporte.backend.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Object errors,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, null, Instant.now());
    }

    public static ApiResponse<Void> success(String code, String message) {
        return success(code, message, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, Object errors) {
        return new ApiResponse<>(false, code, message, null, errors, Instant.now());
    }
}
