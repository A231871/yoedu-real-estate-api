package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.entities.Notification;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.event.ViewingCancelledEvent;
import com.yoedu.yoedurealestateapi.domain.event.ViewingConfirmedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ViewingScheduledEvent;
import com.yoedu.yoedurealestateapi.repository.NotificationRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleViewingScheduled(ViewingScheduledEvent event) {
        try {
            log.info("Handling ViewingScheduledEvent for scheduleId: {}", event.getScheduleId());
            Optional<User> hostOpt = userRepository.findByIdAndDeletedAtIsNull(event.getHostId());
            if (hostOpt.isEmpty()) {
                log.warn("Host user {} not found or deactivated. Skipping notification.", event.getHostId());
                return;
            }

            User host = hostOpt.get();
            String title = "Lịch hẹn xem nhà mới";
            String body = "Khách thuê đã đặt lịch hẹn xem nhà vào " + formatTime(event.getScheduledTime());
            String refType = "VIEWING_SCHEDULE";
            String refId = event.getScheduleId().toString();

            // 1. Save Notification entity in DB transaction
            Notification notification = persistNotification(host, "VIEWING_SCHEDULED", title, body, refType, refId);

            // 2. Perform Network IO (Email & Push) OUTSIDE the DB transaction boundary to prevent HikariCP pool starvation
            dispatchEmailAndPush(host, notification.getId(), title, body, refType, refId);
        } catch (Exception e) {
            log.error("Error processing ViewingScheduledEvent for scheduleId: {}", event.getScheduleId(), e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleViewingConfirmed(ViewingConfirmedEvent event) {
        try {
            log.info("Handling ViewingConfirmedEvent for scheduleId: {}", event.getScheduleId());
            Optional<User> clientOpt = userRepository.findByIdAndDeletedAtIsNull(event.getClientId());
            if (clientOpt.isEmpty()) {
                log.warn("Client user {} not found or deactivated. Skipping notification.", event.getClientId());
                return;
            }

            User client = clientOpt.get();
            String title = "Lịch hẹn đã được xác nhận";
            String body = "Chủ nhà đã xác nhận lịch hẹn xem nhà vào " + formatTime(event.getScheduledTime());
            String refType = "VIEWING_SCHEDULE";
            String refId = event.getScheduleId().toString();

            Notification notification = persistNotification(client, "VIEWING_CONFIRMED", title, body, refType, refId);

            dispatchEmailAndPush(client, notification.getId(), title, body, refType, refId);
        } catch (Exception e) {
            log.error("Error processing ViewingConfirmedEvent for scheduleId: {}", event.getScheduleId(), e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleViewingCancelled(ViewingCancelledEvent event) {
        try {
            log.info("Handling ViewingCancelledEvent for scheduleId: {}", event.getScheduleId());
            // Null-safe comparison against system/cron cancellation
            UUID recipientId = Objects.equals(event.getCancelledBy(), event.getClientId())
                    ? event.getHostId()
                    : event.getClientId();

            Optional<User> recipientOpt = userRepository.findByIdAndDeletedAtIsNull(recipientId);
            if (recipientOpt.isEmpty()) {
                log.warn("Recipient user {} not found or deactivated. Skipping notification.", recipientId);
                return;
            }

            User recipient = recipientOpt.get();
            String title = "Lịch hẹn xem nhà đã bị hủy";
            String reasonText = (event.getReason() != null && !event.getReason().isBlank()) ? event.getReason() : "Không có lý do";
            String body = "Lịch hẹn xem nhà đã bị hủy. Lý do: " + reasonText;
            String refType = "VIEWING_SCHEDULE";
            String refId = event.getScheduleId().toString();

            Notification notification = persistNotification(recipient, "VIEWING_CANCELLED", title, body, refType, refId);

            dispatchEmailAndPush(recipient, notification.getId(), title, body, refType, refId);
        } catch (Exception e) {
            log.error("Error processing ViewingCancelledEvent for scheduleId: {}", event.getScheduleId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification persistNotification(User user, String type, String title, String body, String refType, String refId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setReferenceType(refType);
        notification.setReferenceId(refId);
        notification.setRead(false);
        notification.setPushSent(false);
        return notificationRepository.save(notification);
    }

    private void dispatchEmailAndPush(User recipient, UUID notificationId, String title, String body, String refType, String refId) {
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendEmail(recipient.getEmail(), title, body);
        }

        boolean pushSent = pushNotificationService.sendPushNotification(recipient.getId(), title, body, refType, refId);
        if (pushSent) {
            updatePushSentStatus(notificationId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updatePushSentStatus(UUID notificationId) {
        notificationRepository.markPushSent(notificationId, Instant.now());
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return "";
        }
        return rawTime.replace("T", " ");
    }
}
