package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.notification.PushTokenResponse;
import com.yoedu.yoedurealestateapi.dto.notification.UpsertPushTokenRequest;
import java.util.UUID;

public interface PushTokenService {

    PushTokenResponse upsertPushToken(UUID userId, UpsertPushTokenRequest request);

    void deactivateDeviceToken(UUID userId, String deviceId);
}
