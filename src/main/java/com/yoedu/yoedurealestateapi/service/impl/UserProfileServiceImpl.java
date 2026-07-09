package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.common.exception.UserBannedException;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;
import com.yoedu.yoedurealestateapi.dto.user.UpdateProfileRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;
import com.yoedu.yoedurealestateapi.mapper.UserProfileMapper;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

  private final UserRepository userRepository;
  private final UserProfileMapper userProfileMapper;

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(UUID userId) {
    User user = getUserById(userId);
    return userProfileMapper.toResponse(user);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    User user = getUserById(userId);
    userProfileMapper.updateProfileFromRequest(request, user);
    user = userRepository.save(user);
    return userProfileMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public void checkBanStatus(UUID userId) {
    User user = getUserById(userId);
    if (user.getStatus() == UserStatus.SUSPENDED) {
      throw new UserBannedException("User is suspended and cannot perform this action.");
    }
  }

  private User getUserById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
