package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.notification.NotificationResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);

    long getUnreadCount(UUID userId);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);
}
