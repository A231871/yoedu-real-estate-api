package com.yoedu.yoedurealestateapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = ".*[a-z].*", message = "must contain a lowercase letter")
    @Pattern(regexp = ".*[A-Z].*", message = "must contain an uppercase letter")
    @Pattern(regexp = ".*\\d.*", message = "must contain a digit")
    @Size(min = 8, message = "must be at least 8 characters")
    String password,

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    String fullName,

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String phone
) {}
