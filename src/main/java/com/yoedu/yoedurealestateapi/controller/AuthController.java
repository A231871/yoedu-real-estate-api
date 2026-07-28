package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.auth.LoginRequest;
import com.yoedu.yoedurealestateapi.dto.auth.LoginResponse;
import com.yoedu.yoedurealestateapi.dto.auth.RefreshRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RegisterRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;
import com.yoedu.yoedurealestateapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and authentication")
public class AuthController {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" + // IPv4 (0-255)
            "|([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}" +                                                // IPv6 full
            "|::1" +                                                                                  // IPv6 loopback
            "|([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4})$"                                              // IPv6 compressed
    );
    private static final String FALLBACK_IP = "0.0.0.0";

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user profile with GUEST role and PENDING_VERIFY status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký tài khoản thành công", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Verifies user credentials and returns JWT access and refresh tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        String ipAddress = getClientIp(httpServletRequest);
        String deviceInfo = httpServletRequest.getHeader("User-Agent");

        LoginResponse response = authService.login(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token",
            description = "Validates a refresh token, revokes it, and issues a new access + refresh token pair (rotating strategy)")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpServletRequest) {
        String ipAddress = getClientIp(httpServletRequest);
        String deviceInfo = httpServletRequest.getHeader("User-Agent");

        LoginResponse response = authService.refresh(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Token đã được làm mới thành công", response));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts the real client IP, respecting proxy headers.
     * The extracted value is validated against an IPv4/IPv6 regex.
     * Any non-IP string (e.g., spoofed header values) falls back to {@code 0.0.0.0}.
     *
     * <p>Header priority: X-Forwarded-For (first valid entry) → X-Real-IP → remoteAddr
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            for (String ip : xForwardedFor.split(",")) {
                String trimmed = ip.trim();
                if (!trimmed.isBlank() && !"unknown".equalsIgnoreCase(trimmed)) {
                    return sanitizeIp(trimmed);
                }
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return sanitizeIp(xRealIp.trim());
        }

        return sanitizeIp(request.getRemoteAddr());
    }

    /**
     * Validates that the given string is a well-formed IPv4 or IPv6 address.
     * Returns {@code 0.0.0.0} if validation fails, preventing
     * {@code CAST(... AS inet)} from throwing a PostgreSQL DataException.
     */
    private String sanitizeIp(String ip) {
        if (ip == null || !IP_PATTERN.matcher(ip).matches()) {
            return FALLBACK_IP;
        }
        return ip;
    }
}
