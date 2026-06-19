package com.yoedu.yoedurealestateapi.admin.api.events;

import java.util.UUID;

/**
 * Event emitted by the Admin module when a listing suspension is requested.
 * Dev 1 (Listings) listens to this, performs the db update, and emits ListingSuspendedEvent.
 */
public record ListingSuspensionRequestedEvent(
    UUID listingId,
    UUID adminId,
    String reason
) {}
