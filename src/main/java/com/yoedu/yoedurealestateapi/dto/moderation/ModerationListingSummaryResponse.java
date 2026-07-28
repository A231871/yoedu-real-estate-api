package com.yoedu.yoedurealestateapi.dto.moderation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ModerationListingSummaryResponse(
    UUID id,
    String title,
    String slug,
    String address,
    BigDecimal area,
    BigDecimal currentPrice,
    String listingType,
    String propertyTypeName,
    UUID ownerId,
    String ownerName,
    String status,
    Instant createdAt
) {}
