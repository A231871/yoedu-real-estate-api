package com.yoedu.yoedurealestateapi.redis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ListingViewRedisKeys {

    @Value("${app.listing-views.redis-key-prefix:views:count:}")
    private String countPrefix;

    @Value("${app.listing-views.events-key-prefix:views:events:}")
    private String eventsPrefix;

    @Value("${app.listing-views.dedup-key-prefix:views:dedup:}")
    private String dedupPrefix;

    @Value("${app.listing-views.listing-cache-prefix:listing:approved:}")
    private String listingCachePrefix;

    public String countKey(UUID listingId) {
        return countPrefix + listingId;
    }

    public String eventsKey(UUID listingId) {
        return eventsPrefix + listingId;
    }

    public String dedupKey(UUID listingId, String ipAddress, LocalDate utcDate) {
        return dedupPrefix + listingId + ":" + ipAddress + ":" + utcDate;
    }

    public String listingApprovedKey(UUID listingId) {
        return listingCachePrefix + listingId;
    }

    public String countScanPattern() {
        return countPrefix + "*";
    }

    public UUID listingIdFromCountKey(String key) {
        if (!key.startsWith(countPrefix)) {
            return null;
        }
        try {
            return UUID.fromString(key.substring(countPrefix.length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public Duration ttlUntilEndOfUtcDay() {
        var endOfDay = LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC);
        long seconds = Duration.between(
                java.time.Instant.now(),
                endOfDay.toInstant()).getSeconds();
        return Duration.ofSeconds(Math.max(seconds, 1));
    }
}
