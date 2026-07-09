package com.yoedu.yoedurealestateapi.dto.user;

import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import com.yoedu.yoedurealestateapi.domain.enums.UserRole;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;

import java.time.Instant;

public record UserProfileResponse(
    String id,
    String email,
    String fullName,
    String phone,
    String avatarUrl,
    AuthProvider authProvider,
    UserRole role,
    UserStatus status,
    boolean emailVerified,
    String bio,
    Instant createdAt
) {}
