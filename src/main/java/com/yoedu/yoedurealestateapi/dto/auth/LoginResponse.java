package com.yoedu.yoedurealestateapi.dto.auth;

import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    UserProfileResponse user
) {}
