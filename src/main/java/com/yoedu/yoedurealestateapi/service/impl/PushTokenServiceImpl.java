package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.PushToken;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.dto.notification.PushTokenResponse;
import com.yoedu.yoedurealestateapi.dto.notification.UpsertPushTokenRequest;
import com.yoedu.yoedurealestateapi.repository.PushTokenRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.PushTokenService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushTokenServiceImpl implements PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PushTokenResponse upsertPushToken(UUID userId, UpsertPushTokenRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hoặc tài khoản đã bị khóa"));

        String sanitizedToken = request.getToken() != null ? request.getToken().trim() : "";
        String sanitizedDeviceId = (request.getDeviceId() != null && !request.getDeviceId().isBlank())
                ? request.getDeviceId().trim() : null;

        // Clean up dead/duplicate tokens registered by other users
        if (sanitizedDeviceId != null) {
            pushTokenRepository.deactivateOtherUsersByDeviceId(sanitizedDeviceId, userId);
        }
        if (!sanitizedToken.isBlank()) {
            pushTokenRepository.deactivateOtherUsersByToken(sanitizedToken, userId);
        }

        // Find existing token by (userId, deviceId) or (userId, token) safely using findFirstBy...
        Optional<PushToken> existingOpt = Optional.empty();
        if (sanitizedDeviceId != null) {
            existingOpt = pushTokenRepository.findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(userId, sanitizedDeviceId);
        }
        if (existingOpt.isEmpty()) {
            existingOpt = pushTokenRepository.findFirstByUserIdAndTokenOrderByCreatedAtDesc(userId, sanitizedToken);
        }

        Instant now = Instant.now();
        PushToken tokenEntity = existingOpt.orElseGet(() -> {
            PushToken newToken = new PushToken();
            newToken.setUser(user);
            return newToken;
        });

        tokenEntity.setToken(sanitizedToken);
        tokenEntity.setPlatform(request.getPlatform());
        tokenEntity.setDeviceId(sanitizedDeviceId);
        tokenEntity.setActive(true);
        tokenEntity.setLastUsed(now);

        tokenEntity = pushTokenRepository.save(tokenEntity);
        return mapToResponse(tokenEntity);
    }

    @Override
    @Transactional
    public void deactivateDeviceToken(UUID userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        pushTokenRepository.deactivateByUserIdAndDeviceId(userId, deviceId.trim());
    }

    private PushTokenResponse mapToResponse(PushToken entity) {
        return PushTokenResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .token(entity.getToken())
                .platform(entity.getPlatform())
                .deviceId(entity.getDeviceId())
                .active(entity.isActive())
                .lastUsed(entity.getLastUsed())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
