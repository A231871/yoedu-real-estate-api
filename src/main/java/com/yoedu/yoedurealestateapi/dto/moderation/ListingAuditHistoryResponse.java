package com.yoedu.yoedurealestateapi.dto.moderation;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single Hibernate Envers revision of a Listing.
 * Note: {@code price} is intentionally omitted because {@code ListingPrice}
 * is marked {@code @NotAudited} — accessing it on an Envers snapshot would
 * trigger a {@code LazyInitializationException}.
 */
public record ListingAuditHistoryResponse(
    Number revisionId,
    Instant revisedAt,
    String revisionType,
    UUID listingId,
    String title,
    String status
) {}
