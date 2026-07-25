package com.yoedu.yoedurealestateapi.dto.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken
) {}
