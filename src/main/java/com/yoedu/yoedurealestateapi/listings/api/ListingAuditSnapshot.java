package com.yoedu.yoedurealestateapi.listings.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an audit revision.
 */
public record ListingAuditSnapshot(
    UUID id,
    UUID listingId,
    String action,
    Instant timestamp,
    UUID performedBy
) {}
