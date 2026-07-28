package com.yoedu.yoedurealestateapi.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
    UUID id,
    UUID listingId,
    String listingTitle,
    UUID reporterId,
    String reporterName,
    String reason,
    String description,
    String status,
    String adminNote,
    UUID resolvedById,
    String resolvedByName,
    Instant resolvedAt,
    Instant createdAt
) {}
