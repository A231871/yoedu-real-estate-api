package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.domain.entities.Notification;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.event.ViewingCancelledEvent;
import com.yoedu.yoedurealestateapi.domain.event.ViewingConfirmedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ViewingScheduledEvent;
import com.yoedu.yoedurealestateapi.repository.NotificationRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationEventListener listener;

    private UUID hostId;
    private UUID clientId;
    private UUID scheduleId;
    private User hostUser;
    private User clientUser;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();

        hostUser = new User();
        hostUser.setId(hostId);
        hostUser.setEmail("host@example.com");

        clientUser = new User();
        clientUser.setId(clientId);
        clientUser.setEmail("client@example.com");
    }

    @Test
    void handleViewingScheduled_Success() {
        ViewingScheduledEvent event = ViewingScheduledEvent.builder()
                .scheduleId(scheduleId)
                .listingId(UUID.randomUUID())
                .clientId(clientId)
                .hostId(hostId)
                .scheduledTime("2026-08-01 10:00")
                .note("Want to see balcony")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(hostId)).thenReturn(Optional.of(hostUser));

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUser(hostUser);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(pushNotificationService.sendPushNotification(eq(hostId), any(), any(), any(), any())).thenReturn(true);

        listener.handleViewingScheduled(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification captured = notificationCaptor.getValue();
        assertEquals("VIEWING_SCHEDULED", captured.getType());
        assertEquals("Lịch hẹn xem nhà mới", captured.getTitle());
        assertTrue(captured.getBody().contains("2026-08-01 10:00"));

        verify(emailService).sendEmail(eq("host@example.com"), eq("Lịch hẹn xem nhà mới"), any());
        verify(pushNotificationService).sendPushNotification(eq(hostId), eq("Lịch hẹn xem nhà mới"), any(), eq("VIEWING_SCHEDULE"), eq(scheduleId.toString()));
        verify(notificationRepository).markPushSent(eq(savedNotification.getId()), any());
    }

    @Test
    void handleViewingConfirmed_Success() {
        ViewingConfirmedEvent event = ViewingConfirmedEvent.builder()
                .scheduleId(scheduleId)
                .listingId(UUID.randomUUID())
                .clientId(clientId)
                .hostId(hostId)
                .scheduledTime("2026-08-01 10:00")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(clientId)).thenReturn(Optional.of(clientUser));

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUser(clientUser);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(pushNotificationService.sendPushNotification(eq(clientId), any(), any(), any(), any())).thenReturn(true);

        listener.handleViewingConfirmed(event);

        verify(emailService).sendEmail(eq("client@example.com"), eq("Lịch hẹn đã được xác nhận"), any());
        verify(pushNotificationService).sendPushNotification(eq(clientId), eq("Lịch hẹn đã được xác nhận"), any(), eq("VIEWING_SCHEDULE"), eq(scheduleId.toString()));
        verify(notificationRepository).markPushSent(eq(savedNotification.getId()), any());
    }

    @Test
    void handleViewingCancelled_Success() {
        ViewingCancelledEvent event = ViewingCancelledEvent.builder()
                .scheduleId(scheduleId)
                .listingId(UUID.randomUUID())
                .clientId(clientId)
                .hostId(hostId)
                .cancelledBy(clientId) // Cancelled by client -> notify host
                .reason("Busy on that day")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(hostId)).thenReturn(Optional.of(hostUser));

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUser(hostUser);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        listener.handleViewingCancelled(event);

        verify(emailService).sendEmail(eq("host@example.com"), eq("Lịch hẹn xem nhà đã bị hủy"), any());
        verify(pushNotificationService).sendPushNotification(eq(hostId), eq("Lịch hẹn xem nhà đã bị hủy"), any(), eq("VIEWING_SCHEDULE"), eq(scheduleId.toString()));
    }

    @Test
    void handleViewingCancelled_SystemCancelled_NullSafe() {
        ViewingCancelledEvent event = ViewingCancelledEvent.builder()
                .scheduleId(scheduleId)
                .listingId(UUID.randomUUID())
                .clientId(clientId)
                .hostId(hostId)
                .cancelledBy(null) // System/cron cancellation -> notify client
                .reason("Expired")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(clientId)).thenReturn(Optional.of(clientUser));

        Notification savedNotification = new Notification();
        savedNotification.setId(UUID.randomUUID());
        savedNotification.setUser(clientUser);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        listener.handleViewingCancelled(event);

        verify(emailService).sendEmail(eq("client@example.com"), eq("Lịch hẹn xem nhà đã bị hủy"), any());
        verify(pushNotificationService).sendPushNotification(eq(clientId), eq("Lịch hẹn xem nhà đã bị hủy"), any(), eq("VIEWING_SCHEDULE"), eq(scheduleId.toString()));
    }
}
