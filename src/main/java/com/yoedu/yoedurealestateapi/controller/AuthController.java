package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.auth.*;
import com.yoedu.yoedurealestateapi.service.AuthService;
import com.yoedu.yoedurealestateapi.security.AppJwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.apache.commons.lang3.tuple.Pair;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    private final AuthService authService;
    private final AppJwtProperties appJwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Stores a pending registration in Redis and sends a verification email; the account is only created once the user verifies")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.successMessage("Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản."));
    }

    @GetMapping("/verify")
    @Operation(
        summary = "Verify a pending registration",
        description = "Validates the verification token, creates the user account, and returns JWT tokens"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> verify(
            @RequestParam @NotBlank(message = "Verification token is required")
            String token) {

        Pair<AuthResponse, String> result = authService.verifyRegistration(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRight()).toString())
                .body(ApiResponse.success("Xác minh tài khoản thành công", result.getLeft()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies user credentials and returns JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        Pair<AuthResponse, String> result = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRight()).toString())
                .body(ApiResponse.success("Đăng nhập thành công", result.getLeft()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token",
            description = "Validates a refresh token, revokes it, and issues a new access + refresh token pair (rotating strategy)")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        Pair<AuthResponse, String> result = authService.refresh(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(result.getRight()).toString())
                .body(ApiResponse.success("Token đã được làm mới thành công", result.getLeft()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token",
            description = "Revoke the refresh token from that device")
    public ResponseEntity<ApiResponse<Void>> logout(
        @Parameter(hidden = true)
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.revokeRefreshToken(refreshToken);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie().toString());
        return ResponseEntity.ok().build();
    }

    // Helper method for building a cookie
    private ResponseCookie buildRefreshTokenCookie(String refreshToken) {
        return ResponseCookie
            .from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(appJwtProperties.refreshTokenTtlDays() * 24 * 60 * 60)
            .sameSite("Strict")
            .build();
    }

    private ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie
            .from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .sameSite("Strict")
            .build();
    }
}
