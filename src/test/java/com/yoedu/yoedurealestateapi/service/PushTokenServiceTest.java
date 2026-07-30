package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.PushToken;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.PushPlatform;
import com.yoedu.yoedurealestateapi.dto.notification.PushTokenResponse;
import com.yoedu.yoedurealestateapi.dto.notification.UpsertPushTokenRequest;
import com.yoedu.yoedurealestateapi.repository.PushTokenRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.impl.PushTokenServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PushTokenServiceImpl pushTokenService;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = new User();
        activeUser.setId(userId);
        activeUser.setEmail("user@example.com");
    }

    @Test
    void upsertPushToken_NewToken_Success() {
        UpsertPushTokenRequest request = UpsertPushTokenRequest.builder()
                .token("  fcm-token-123  ")
                .platform(PushPlatform.FCM)
                .deviceId("  device-abc  ")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(pushTokenRepository.findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, "device-abc")).thenReturn(Optional.empty());
        when(pushTokenRepository.findFirstByUserIdAndTokenOrderByCreatedAtDesc(userId, "fcm-token-123")).thenReturn(Optional.empty());

        PushToken savedToken = new PushToken();
        savedToken.setId(UUID.randomUUID());
        savedToken.setUser(activeUser);
        savedToken.setToken("fcm-token-123");
        savedToken.setPlatform(PushPlatform.FCM);
        savedToken.setDeviceId("device-abc");
        savedToken.setActive(true);
        savedToken.setLastUsed(Instant.now());
        savedToken.setCreatedAt(Instant.now());

        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(savedToken);

        PushTokenResponse response = pushTokenService.upsertPushToken(userId, request);

        assertNotNull(response);
        assertEquals("fcm-token-123", response.getToken());
        assertEquals(PushPlatform.FCM, response.getPlatform());
        assertEquals("device-abc", response.getDeviceId());
        assertTrue(response.isActive());

        verify(pushTokenRepository).deactivateOtherUsersByDeviceId("device-abc", userId);
        verify(pushTokenRepository).deactivateOtherUsersByToken("fcm-token-123", userId);
    }

    @Test
    void upsertPushToken_ExistingToken_UpdatesAndReactivates() {
        UpsertPushTokenRequest request = UpsertPushTokenRequest.builder()
                .token("fcm-token-updated")
                .platform(PushPlatform.FCM)
                .deviceId("device-abc")
                .build();

        PushToken existingToken = new PushToken();
        existingToken.setId(UUID.randomUUID());
        existingToken.setUser(activeUser);
        existingToken.setToken("fcm-token-old");
        existingToken.setPlatform(PushPlatform.FCM);
        existingToken.setDeviceId("device-abc");
        existingToken.setActive(false);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(pushTokenRepository.findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, "device-abc")).thenReturn(Optional.of(existingToken));
        when(pushTokenRepository.save(any(PushToken.class))).thenReturn(existingToken);

        PushTokenResponse response = pushTokenService.upsertPushToken(userId, request);

        assertNotNull(response);
        assertEquals("fcm-token-updated", existingToken.getToken());
        assertTrue(existingToken.isActive());
        assertNotNull(existingToken.getLastUsed());
    }

    @Test
    void upsertPushToken_SoftDeletedUser_ThrowsNotFoundException() {
        UpsertPushTokenRequest request = UpsertPushTokenRequest.builder()
                .token("fcm-token-123")
                .platform(PushPlatform.FCM)
                .deviceId("device-abc")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> pushTokenService.upsertPushToken(userId, request));
        verify(pushTokenRepository, never()).save(any());
    }

    @Test
    void deactivateDeviceToken_Success() {
        pushTokenService.deactivateDeviceToken(userId, "device-abc");
        verify(pushTokenRepository).deactivateByUserIdAndDeviceId(userId, "device-abc");
    }

    @Test
    void deactivateDeviceToken_NullOrBlank_NoOp() {
        pushTokenService.deactivateDeviceToken(userId, "");
        pushTokenService.deactivateDeviceToken(userId, null);
        verify(pushTokenRepository, never()).deactivateByUserIdAndDeviceId(any(), any());
    }
}
