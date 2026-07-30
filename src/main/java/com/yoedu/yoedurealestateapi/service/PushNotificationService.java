package com.yoedu.yoedurealestateapi.service;

import java.util.UUID;

public interface PushNotificationService {
    boolean sendPushNotification(UUID userId, String title, String body, String referenceType, String referenceId);
}
