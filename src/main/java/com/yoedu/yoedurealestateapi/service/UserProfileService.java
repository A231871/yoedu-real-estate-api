package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.UpdateProfileRequest;
import com.yoedu.yoedurealestateapi.dto.UserProfileResponse;

import java.util.UUID;

public interface UserProfileService {

  UserProfileResponse getProfile(UUID userId);

  UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

  void checkBanStatus(UUID userId);
}
