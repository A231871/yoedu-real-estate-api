package com.yoedu.yoedurealestateapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
