package com.yoedu.yoedurealestateapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record VerifyRegistrationRequest(
    @NotBlank(message = "Verification token is required")
    String token
) {
}
