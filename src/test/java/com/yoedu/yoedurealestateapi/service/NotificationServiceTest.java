package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Notification;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.dto.notification.NotificationResponse;
import com.yoedu.yoedurealestateapi.repository.NotificationRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.impl.NotificationServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

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
    void getUserNotifications_Success() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(activeUser);
        notification.setType("VIEWING_SCHEDULED");
        notification.setTitle("Lịch hẹn xem nhà mới");
        notification.setBody("Nội dung thông báo");
        notification.setReferenceType("VIEWING_SCHEDULE");
        notification.setReferenceId(UUID.randomUUID().toString());
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());

        Page<Notification> entityPage = new PageImpl<>(List.of(notification));
        Pageable pageable = PageRequest.of(0, 20);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class))).thenReturn(entityPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("VIEWING_SCHEDULED", result.getContent().get(0).getType());
        assertEquals("Lịch hẹn xem nhà mới", result.getContent().get(0).getTitle());
    }

    @Test
    void getUserNotifications_CapsExcessivePageSize() {
        Pageable excessivePageable = PageRequest.of(0, 500);
        Page<Notification> emptyPage = Page.empty();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class))).thenReturn(emptyPage);

        notificationService.getUserNotifications(userId, excessivePageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), pageableCaptor.capture());

        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getUserNotifications_SoftDeletedUser_ThrowsNotFound() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.getUserNotifications(userId, PageRequest.of(0, 20)));
    }

    @Test
    void getUnreadCount_Success() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUser(activeUser);
        notification.setType("VIEWING_SCHEDULED");
        notification.setTitle("Title");
        notification.setBody("Body");
        notification.setRead(false);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(userId, notificationId);

        assertNotNull(response);
        assertTrue(response.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotificationNotFoundOrIDOR_ThrowsNotFound() {
        UUID notificationId = UUID.randomUUID();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));
        when(notificationRepository.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> notificationService.markAsRead(userId, notificationId));
    }

    @Test
    void markAllAsRead_Success() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(activeUser));

        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsReadByUserId(userId);
    }
}
