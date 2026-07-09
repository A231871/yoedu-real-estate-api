package com.yoedu.yoedurealestateapi.listings.api.events;

import java.util.UUID;

/**
 * Event emitted by the Listings module after a listing has been successfully suspended.
 * Dev 2 (Bookings) listens to this to cancel active viewing schedules.
 */
public record ListingSuspendedEvent(
    UUID listingId,
    UUID suspendedByAdminId
) {}
