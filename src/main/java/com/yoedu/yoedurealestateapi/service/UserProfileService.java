package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.user.UpdateProfileRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

  UserProfileResponse getProfile(UUID userId);

  UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

  void checkBanStatus(UUID userId);
}
