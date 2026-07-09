package com.yoedu.yoedurealestateapi.listings.api;

import java.util.UUID;

/**
 * Public API for the Listings module.
 * Other modules must depend on this interface, NOT the internal service implementation.
 */
public interface ListingApi {
    /**
     * Retrieve a read-only snapshot of a listing for inter-module operations.
     */
    ListingSnapshot getListingSnapshot(UUID listingId);
}
