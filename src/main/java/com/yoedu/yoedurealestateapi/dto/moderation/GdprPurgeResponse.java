package com.yoedu.yoedurealestateapi.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record GdprPurgeResponse(
    UUID userId,
    Instant purgedAt,
    String anonymizedEmail,
    int auditRecordsScrubbed
) {}
