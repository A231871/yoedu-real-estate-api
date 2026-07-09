package com.yoedu.yoedurealestateapi.listings.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable snapshot of a listing for cross-module reads.
 */
public record ListingSnapshot(
    UUID id,
    String title,
    BigDecimal price,
    String status
) {}
