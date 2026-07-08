package com.yoedu.yoedurealestateapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

  @NotBlank(message = "Full name must not be blank")
  @Size(max = 150, message = "Full name must be less than 150 characters")
  private String fullName;

  @Size(max = 20, message = "Phone must be less than 20 characters")
  private String phone;

  @Size(max = 500, message = "Avatar URL must be less than 500 characters")
  private String avatarUrl;

  private String bio;
}
