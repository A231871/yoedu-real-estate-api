package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.auth.LoginRequest;
import com.yoedu.yoedurealestateapi.dto.auth.LoginResponse;
import com.yoedu.yoedurealestateapi.dto.auth.RefreshRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RegisterRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;

public interface AuthService {

    UserProfileResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo);

    /**
     * Validates an existing refresh token, revokes it, and issues a new
     * rotating access + refresh token pair.
     *
     * @param request    contains the raw refresh token string
     * @param ipAddress  client IP (for auditing the new token)
     * @param deviceInfo User-Agent (for auditing the new token)
     * @return new access token and a new refresh token
     */
    LoginResponse refresh(RefreshRequest request, String ipAddress, String deviceInfo);
}
