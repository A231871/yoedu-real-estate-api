package com.yoedu.yoedurealestateapi.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorName,
    String action,
    String entityType,
    String entityId,
    String oldValue,
    String newValue,
    String ipAddress,
    String userAgent,
    Instant createdAt
) {}
