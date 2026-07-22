package com.yoedu.yoedurealestateapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.UserBannedException;
import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import com.yoedu.yoedurealestateapi.domain.enums.UserRole;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;
import com.yoedu.yoedurealestateapi.dto.auth.LoginRequest;
import com.yoedu.yoedurealestateapi.dto.auth.LoginResponse;
import com.yoedu.yoedurealestateapi.dto.auth.RefreshRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RegisterRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;
import com.yoedu.yoedurealestateapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

class AuthControllerTest {

    private AuthService authService;
    private AuthController authController;
    private HttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
        servletRequest = mock(HttpServletRequest.class);
    }

    private UserProfileResponse buildProfile(UserStatus status) {
        return new UserProfileResponse(
                UUID.randomUUID().toString(), "test@test.com", "Test User", "0901234567",
                null, AuthProvider.LOCAL, UserRole.GUEST, status, false, null, Instant.now()
        );
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("test@test.com", "Password1", "Test User", "0901234567");
        UserProfileResponse response = buildProfile(UserStatus.PENDING_VERIFY);
        when(authService.register(any())).thenReturn(response);

        ResponseEntity<ApiResponse<UserProfileResponse>> responseEntity = authController.register(request);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("Đăng ký tài khoản thành công", responseEntity.getBody().message());
        assertEquals("test@test.com", responseEntity.getBody().data().email());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@test.com", "Password1");
        LoginResponse response = new LoginResponse("access-token-123", "refresh-token-123", buildProfile(UserStatus.ACTIVE));

        when(authService.login(any(), any(), any())).thenReturn(response);
        when(servletRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<ApiResponse<LoginResponse>> responseEntity = authController.login(request, servletRequest);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("Đăng nhập thành công", responseEntity.getBody().message());
        assertEquals("access-token-123", responseEntity.getBody().data().accessToken());
    }

    @Test
    void login_BadCredentials_Throws() {
        LoginRequest request = new LoginRequest("test@test.com", "wrong");
        when(authService.login(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Username or password is invalid"));
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThrows(BadCredentialsException.class, () -> authController.login(request, servletRequest));
    }

    @Test
    void login_PendingVerify_Throws() {
        LoginRequest request = new LoginRequest("test@test.com", "Password1");
        when(authService.login(any(), any(), any()))
                .thenThrow(new UserBannedException("Tài khoản của bạn chưa được xác minh email."));
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThrows(UserBannedException.class, () -> authController.login(request, servletRequest));
    }

    @Test
    void refresh_Success() {
        RefreshRequest request = new RefreshRequest("valid-refresh-token");
        LoginResponse response = new LoginResponse("new-access-token", "new-refresh-token", buildProfile(UserStatus.ACTIVE));

        when(authService.refresh(any(), any(), any())).thenReturn(response);
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<ApiResponse<LoginResponse>> responseEntity = authController.refresh(request, servletRequest);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("new-access-token", responseEntity.getBody().data().accessToken());
        assertEquals("new-refresh-token", responseEntity.getBody().data().refreshToken());
    }

    @Test
    void refresh_InvalidToken_Throws() {
        RefreshRequest request = new RefreshRequest("expired-token");
        when(authService.refresh(any(), any(), any()))
                .thenThrow(new BadCredentialsException("Refresh token has expired."));
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThrows(BadCredentialsException.class, () -> authController.refresh(request, servletRequest));
    }

    @Test
    void login_InvalidIp_FallsBackToDefault() {
        LoginRequest request = new LoginRequest("test@test.com", "Password1");
        LoginResponse response = new LoginResponse("access-token", "refresh-token", buildProfile(UserStatus.ACTIVE));

        when(authService.login(any(), any(), any())).thenReturn(response);
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("999.999.999.999");
        when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<ApiResponse<LoginResponse>> responseEntity = authController.login(request, servletRequest);
        assertEquals(200, responseEntity.getStatusCode().value());
    }
}
