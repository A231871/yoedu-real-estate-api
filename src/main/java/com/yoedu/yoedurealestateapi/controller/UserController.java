package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.user.UpdateProfileRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;
import com.yoedu.yoedurealestateapi.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for user profile management")
public class UserController {

  private final UserProfileService userProfileService;

  @GetMapping
  @Operation(summary = "Get User Profile", description = "Retrieves the profile of the currently authenticated user")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
      @AuthenticationPrincipal String username) {
    // Assuming the username in the JWT is the UUID string
    UUID userId = UUID.fromString(username);
    UserProfileResponse response = userProfileService.getProfile(userId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @PutMapping
  @Operation(summary = "Update User Profile", description = "Updates contact information and avatar for the current user")
  public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
      @AuthenticationPrincipal String username,
      @Valid @RequestBody UpdateProfileRequest request) {
    UUID userId = UUID.fromString(username);
    UserProfileResponse response = userProfileService.updateProfile(userId, request);
    return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
  }
}
