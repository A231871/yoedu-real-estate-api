package com.yoedu.yoedurealestateapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.notification.NotificationResponse;
import com.yoedu.yoedurealestateapi.service.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationController notificationController;
    private Authentication authentication;
    private UUID validUserId;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        notificationController = new NotificationController(notificationService);
        authentication = mock(Authentication.class);

        validUserId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        when(authentication.getName()).thenReturn(validUserId.toString());
    }

    @Test
    void getUserNotifications_Success() {
        NotificationResponse item = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .type("VIEWING_SCHEDULED")
                .title("Lịch hẹn mới")
                .body("Chi tiết lịch hẹn")
                .referenceType("VIEWING_SCHEDULE")
                .referenceId(UUID.randomUUID().toString())
                .read(false)
                .createdAt(Instant.now())
                .build();

        Page<NotificationResponse> page = new PageImpl<>(List.of(item));
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationService.getUserNotifications(eq(validUserId), any(Pageable.class))).thenReturn(page);

        ResponseEntity<ApiResponse<Page<NotificationResponse>>> responseEntity =
                notificationController.getUserNotifications(pageable, authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals(1, responseEntity.getBody().data().getTotalElements());
        assertEquals("Lịch hẹn mới", responseEntity.getBody().data().getContent().get(0).getTitle());
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationService.getUnreadCount(validUserId)).thenReturn(3L);

        ResponseEntity<ApiResponse<Long>> responseEntity =
                notificationController.getUnreadCount(authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals(3L, responseEntity.getBody().data());
    }

    @Test
    void markAsRead_Success() {
        UUID notificationId = UUID.randomUUID();
        NotificationResponse responseDto = NotificationResponse.builder()
                .id(notificationId)
                .read(true)
                .build();

        when(notificationService.markAsRead(validUserId, notificationId)).thenReturn(responseDto);

        ResponseEntity<ApiResponse<NotificationResponse>> responseEntity =
                notificationController.markAsRead(notificationId, authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());
        assertEquals(true, responseEntity.getBody().data().isRead());
    }

    @Test
    void markAllAsRead_Success() {
        ResponseEntity<ApiResponse<Void>> responseEntity =
                notificationController.markAllAsRead(authentication);

        assertEquals(200, responseEntity.getStatusCode().value());
        assertNotNull(responseEntity.getBody());
        assertEquals(true, responseEntity.getBody().success());

        verify(notificationService).markAllAsRead(validUserId);
    }
}
