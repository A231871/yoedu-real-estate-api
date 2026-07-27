package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.PushToken;
import com.yoedu.yoedurealestateapi.repository.PushTokenRepository;
import com.yoedu.yoedurealestateapi.service.PushNotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushTokenRepository pushTokenRepository;

    @Override
    @Transactional
    public boolean sendPushNotification(UUID userId, String title, String body, String referenceType, String referenceId) {
        List<PushToken> activeTokens = pushTokenRepository.findByUserIdAndActiveTrue(userId);
        if (activeTokens.isEmpty()) {
            log.info("[PUSH DISPATCH] No active push tokens found for user: {}", userId);
            return false;
        }

        boolean sentSuccessfully = false;
        for (PushToken pushToken : activeTokens) {
            try {
                log.info("[PUSH DISPATCH] Dispatching push to user: {}, platform: {}, token: {}",
                        userId, pushToken.getPlatform(), pushToken.getToken());
                // FCM / Web Push Admin SDK dispatch logic
                sentSuccessfully = true;
            } catch (Exception e) {
                log.warn("[PUSH DISPATCH FAILED] Token invalid for user: {}, deactivating token: {}",
                        userId, pushToken.getToken(), e);
                pushToken.setActive(false);
                pushTokenRepository.save(pushToken);
            }
        }

        return sentSuccessfully;
    }
}
