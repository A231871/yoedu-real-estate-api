package com.yoedu.yoedurealestateapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.domain.enums.PushPlatform;
import com.yoedu.yoedurealestateapi.dto.notification.PushTokenResponse;
import com.yoedu.yoedurealestateapi.dto.notification.UpsertPushTokenRequest;
import com.yoedu.yoedurealestateapi.service.PushTokenService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

class PushTokenControllerTest {

    private PushTokenService pushTokenService;
    private PushTokenController pushTokenController;
    private Authentication authentication;
    private UUID validUserId;

    @BeforeEach
    void setUp() {
        pushTokenService = mock(PushTokenService.class);
        pushTokenController = new PushTokenController(pushTokenService);
        authentication = mock(Authentication.class);

        validUserId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        when(authentication.getName()).thenReturn(validUserId.toString());
    }

    @Test
    void upsertPushToken_Success() {
        UpsertPushTokenRequest request = UpsertPushTokenRequest.builder()
                .token("sample-token")
                .platform(PushPlatform.FCM)
                .deviceId("device-123")
                .build();

        PushTokenResponse expectedResponse = PushTokenResponse.builder()
                .id(UUID.randomUUID())
                .userId(validUserId)
                .token("sample-token")
                .platform(PushPlatform.FCM)
                .deviceId("device-123")
                .active(true)
                .lastUsed(Instant.now())
                .createdAt(Instant.now())
                .build();

        when(pushTokenService.upsertPushToken(eq(validUserId), eq(request))).thenReturn(expectedResponse);

        ResponseEntity<ApiResponse<PushTokenResponse>> responseEntity =
                pushTokenController.upsertPushToken(request, authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("Lưu token thiết bị thành công", responseEntity.getBody().message());
        assertEquals("sample-token", responseEntity.getBody().data().getToken());
    }

    @Test
    void deactivatePushToken_Success() {
        ResponseEntity<ApiResponse<Void>> responseEntity =
                pushTokenController.deactivatePushToken("device-123", authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals("Hủy token thiết bị thành công", responseEntity.getBody().message());

        verify(pushTokenService).deactivateDeviceToken(validUserId, "device-123");
    }
}
