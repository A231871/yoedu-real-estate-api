package com.yoedu.yoedurealestateapi.dto.auth;

import com.yoedu.yoedurealestateapi.security.password.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    String email,

    /**
     * Minimum 8 characters, at least one uppercase letter, one lowercase letter,
     * and one digit. Special characters are welcome but not required.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "must be at least 8 characters")
    @ValidPassword
    String password,

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    String fullName,

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phone
) {}
