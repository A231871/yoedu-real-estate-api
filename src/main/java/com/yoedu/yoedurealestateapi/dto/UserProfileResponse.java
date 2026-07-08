package com.yoedu.yoedurealestateapi.dto;

import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserProfileResponse {

  private UUID id;
  private String email;
  private String fullName;
  private String phone;
  private String avatarUrl;
  private AuthProvider authProvider;
  private List<String> roles;
  private User.Status status;
  private boolean emailVerified;
  private String bio;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
