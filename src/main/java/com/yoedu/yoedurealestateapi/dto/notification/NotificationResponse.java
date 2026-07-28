package com.yoedu.yoedurealestateapi.dto.notification;

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
public class NotificationResponse {

    private UUID id;
    private String type;
    private String title;
    private String body;
    private String referenceType;
    private String referenceId;
    private boolean read;
    private Instant createdAt;
}
