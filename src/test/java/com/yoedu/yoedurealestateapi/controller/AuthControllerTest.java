package com.yoedu.yoedurealestateapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.dto.auth.AuthResponse;
import com.yoedu.yoedurealestateapi.dto.auth.LoginRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RefreshRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RegisterRequest;

import com.yoedu.yoedurealestateapi.service.AuthService;
import com.yoedu.yoedurealestateapi.security.AppJwtProperties;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

class AuthControllerTest {

    private AuthService authService;
    private AppJwtProperties appJwtProperties;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        appJwtProperties = mock(AppJwtProperties.class);
        when(appJwtProperties.refreshTokenTtlDays()).thenReturn(7L);
        authController = new AuthController(authService, appJwtProperties);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("test@test.com", "Password1", "Test User", "0901234567");
        doNothing().when(authService).register(any());

        ResponseEntity<ApiResponse<Void>> responseEntity = authController.register(request);

        assertEquals(202, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
    }

    @Test
    void register_DuplicateEmail_Throws() {
        RegisterRequest request = new RegisterRequest("test@test.com", "Password1", "Test User", "0901234567");
        doThrow(new BadRequestException("This email has already been taken")).when(authService).register(any());

        assertThrows(BadRequestException.class, () -> authController.register(request));
    }

    @Test
    void verify_Success() {
        String token = "valid-verification-token";
        AuthResponse response = new AuthResponse("access-token-123", "user-id-123", "test@test.com");
        when(authService.verifyRegistration(any())).thenReturn(Pair.of(response, "refresh-token-123"));

        ResponseEntity<ApiResponse<AuthResponse>> responseEntity = authController.verify(token);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("access-token-123", responseEntity.getBody().data().accessToken());
        String cookie = responseEntity.getHeaders().getFirst("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("refresh-token-123"));
    }

    @Test
    void verify_InvalidToken_Throws() {
        String token = "garbage-token";
        when(authService.verifyRegistration(any()))
                .thenThrow(new BadCredentialsException("Verification link is invalid or has expired"));

        assertThrows(BadCredentialsException.class, () -> authController.verify(token));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@test.com", "Password1");
        AuthResponse response = new AuthResponse("access-token-123", "user-id-123", "test@test.com");

        when(authService.login(any())).thenReturn(Pair.of(response, "refresh-token-123"));

        ResponseEntity<ApiResponse<AuthResponse>> responseEntity = authController.login(request);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("Đăng nhập thành công", responseEntity.getBody().message());
        assertEquals("access-token-123", responseEntity.getBody().data().accessToken());
        String cookie = responseEntity.getHeaders().getFirst("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("refresh-token-123"));
    }

    @Test
    void login_BadCredentials_Throws() {
        LoginRequest request = new LoginRequest("test@test.com", "wrong");
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authController.login(request));
    }

    @Test
    void refresh_Success() {
        RefreshRequest request = new RefreshRequest("valid-refresh-token");
        AuthResponse response = new AuthResponse("new-access-token", "user-id-123", "test@test.com");

        when(authService.refresh(any())).thenReturn(Pair.of(response, "new-refresh-token"));

        ResponseEntity<ApiResponse<AuthResponse>> responseEntity = authController.refresh(request);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("new-access-token", responseEntity.getBody().data().accessToken());
        String cookie = responseEntity.getHeaders().getFirst("Set-Cookie");
        assertNotNull(cookie);
        assertTrue(cookie.contains("new-refresh-token"));
    }

    @Test
    void refresh_InvalidToken_Throws() {
        RefreshRequest request = new RefreshRequest("expired-token");
        when(authService.refresh(any()))
                .thenThrow(new BadCredentialsException("Refresh token has expired."));

        assertThrows(BadCredentialsException.class, () -> authController.refresh(request));
    }
}
