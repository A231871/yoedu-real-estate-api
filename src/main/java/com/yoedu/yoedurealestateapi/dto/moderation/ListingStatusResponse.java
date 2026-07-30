package com.yoedu.yoedurealestateapi.dto.moderation;

import java.util.UUID;

public record ListingStatusResponse(
    UUID listingId,
    String status
) {}
