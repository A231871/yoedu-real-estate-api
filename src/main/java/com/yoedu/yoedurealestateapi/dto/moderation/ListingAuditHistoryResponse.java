package com.yoedu.yoedurealestateapi.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ListingAuditHistoryResponse(
    Number revisionId,
    Instant revisedAt,
    String revisionType,
    UUID listingId,
    String title,
    String status
) {}
