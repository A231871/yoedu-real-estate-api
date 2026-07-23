package com.yoedu.yoedurealestateapi.domain.events;

import java.util.List;
import java.util.UUID;

public record ListingReportedEvent(
    UUID listingId,
    UUID reporterId,
    String reason,
    String description,
    List<String> evidenceUrls
) {}
