package com.yoedu.yoedurealestateapi.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ApiResponse<T>(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    boolean success,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String message,

    T data,

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    public static ApiResponse<Void> successMessage(String message) {
        return success(message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
