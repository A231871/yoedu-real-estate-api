package com.yoedu.yoedurealestateapi.dto.notification;

import com.yoedu.yoedurealestateapi.domain.enums.PushPlatform;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenResponse {

    private UUID id;
    private UUID userId;
    private String token;
    private PushPlatform platform;
    private String deviceId;
    private boolean active;
    private Instant lastUsed;
    private Instant createdAt;
}
