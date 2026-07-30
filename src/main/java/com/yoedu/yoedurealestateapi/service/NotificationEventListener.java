package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.entities.Notification;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspendedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ReportResolvedEvent;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private TransactionTemplate transactionTemplate;

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

            Notification notification = persistNotificationInTransaction(host, "VIEWING_SCHEDULED", title, body, refType, refId);
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

            Notification notification = persistNotificationInTransaction(client, "VIEWING_CONFIRMED", title, body, refType, refId);
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

            Notification notification = persistNotificationInTransaction(recipient, "VIEWING_CANCELLED", title, body, refType, refId);
            dispatchEmailAndPush(recipient, notification.getId(), title, body, refType, refId);
        } catch (Exception e) {
            log.error("Error processing ViewingCancelledEvent for scheduleId: {}", event.getScheduleId(), e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleListingSuspended(ListingSuspendedEvent event) {
        if (event.getOwnerId() == null) {
            log.warn("ListingSuspendedEvent for listing {} has no owner — skipping notification.", event.getListingId());
            return;
        }
        try {
            log.info("Handling ListingSuspendedEvent for listingId: {}", event.getListingId());
            userRepository.findByIdAndDeletedAtIsNull(event.getOwnerId()).ifPresentOrElse(owner -> {
                String listingTitle = event.getListingTitle() != null ? event.getListingTitle() : "Bất động sản";
                String reasonText = (event.getReason() != null && !event.getReason().isBlank()) ? event.getReason() : "Vi phạm quy định đăng tin";
                String title = "Tin đăng của bạn đã bị tạm dừng";
                String body = "Tin đăng \"" + listingTitle + "\" đã bị tạm dừng. Lý do: " + reasonText;
                String refType = "LISTING";
                String refId = event.getListingId().toString();

                Notification notification = persistNotificationInTransaction(owner, "LISTING_SUSPENDED", title, body, refType, refId);
                dispatchEmailAndPush(owner, notification.getId(), title, body, refType, refId);
            }, () -> log.warn("Owner {} not found for ListingSuspendedEvent. Skipping notification.", event.getOwnerId()));
        } catch (Exception e) {
            log.error("Error processing ListingSuspendedEvent for listingId: {}", event.getListingId(), e);
        }
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportResolved(ReportResolvedEvent event) {
        if (event.getOwnerId() == null) {
            log.warn("ReportResolvedEvent for report {} has no owner — skipping notification.", event.getReportId());
            return;
        }
        try {
            log.info("Handling ReportResolvedEvent for reportId: {}", event.getReportId());
            userRepository.findByIdAndDeletedAtIsNull(event.getOwnerId()).ifPresentOrElse(owner -> {
                String listingTitle = event.getListingTitle() != null ? event.getListingTitle() : "Bất động sản";
                String resolution = event.getResolution() != null ? event.getResolution() : "ĐÃ XỬ LÝ";
                String title = "Báo cáo vi phạm đã được xử lý";
                String body = "Báo cáo về tin đăng \"" + listingTitle + "\" đã được xử lý: " + resolution;
                String refType = "REPORT";
                String refId = event.getReportId().toString();

                Notification notification = persistNotificationInTransaction(owner, "REPORT_RESOLVED", title, body, refType, refId);
                dispatchEmailAndPush(owner, notification.getId(), title, body, refType, refId);
            }, () -> log.warn("Owner {} not found for ReportResolvedEvent. Skipping notification.", event.getOwnerId()));
        } catch (Exception e) {
            log.error("Error processing ReportResolvedEvent for reportId: {}", event.getReportId(), e);
        }
    }

    /**
     * Executes notification persistence inside a transaction when TransactionTemplate is available,
     * or directly when un-managed (e.g. in Mockito unit tests).
     */
    public Notification persistNotificationInTransaction(User user, String type, String title, String body, String refType, String refId) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> createAndSaveNotification(user, type, title, body, refType, refId));
        }
        return createAndSaveNotification(user, type, title, body, refType, refId);
    }

    private Notification createAndSaveNotification(User user, String type, String title, String body, String refType, String refId) {
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

    /**
     * Executes push status update inside a transaction when TransactionTemplate is available.
     */
    public void updatePushSentStatusInTransaction(UUID notificationId) {
        if (transactionTemplate != null) {
            transactionTemplate.executeWithoutResult(status ->
                notificationRepository.markPushSent(notificationId, Instant.now())
            );
        } else {
            notificationRepository.markPushSent(notificationId, Instant.now());
        }
    }

    /**
     * Dispatches email and push notifications safely. Wraps email execution in a try-catch block
     * so SMTP failures never abort push notification delivery.
     */
    private void dispatchEmailAndPush(User recipient, UUID notificationId, String title, String body, String refType, String refId) {
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            try {
                emailService.sendEmail(recipient.getEmail(), title, body);
            } catch (Exception e) {
                log.error("Failed to send email notification to {}: {}", recipient.getEmail(), e.getMessage());
            }
        }

        try {
            boolean pushSent = pushNotificationService.sendPushNotification(recipient.getId(), title, body, refType, refId);
            if (pushSent) {
                updatePushSentStatusInTransaction(notificationId);
            }
        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", recipient.getId(), e.getMessage());
        }
    }

    private String formatTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return "";
        }
        return rawTime.replace("T", " ");
    }
}
