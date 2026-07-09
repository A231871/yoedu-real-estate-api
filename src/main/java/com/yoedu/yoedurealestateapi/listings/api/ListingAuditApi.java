package com.yoedu.yoedurealestateapi.listings.api;

import java.util.List;
import java.util.UUID;

/**
 * Public API for accessing Listing Audit logs.
 * Preserves Modulith encapsulation so Dev 3 does not directly query listings_aud using Envers.
 */
public interface ListingAuditApi {
    List<ListingAuditSnapshot> getRecentAudits(UUID listingId);
}
