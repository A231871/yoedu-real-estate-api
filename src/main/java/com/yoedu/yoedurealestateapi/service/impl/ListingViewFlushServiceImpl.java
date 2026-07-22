package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.dto.listingview.ViewEventPayload;
import com.yoedu.yoedurealestateapi.redis.ListingViewRedisKeys;
import com.yoedu.yoedurealestateapi.repository.ListingViewRepository;
import com.yoedu.yoedurealestateapi.service.ListingViewFlushService;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingViewFlushServiceImpl implements ListingViewFlushService {

    /**
     * Atomically transfers buffered view count and events from Redis to the caller.
     *
     * <p>Fix: when KEYS[1] (countKey) is nil or zero, we also DEL KEYS[2] (eventsKey)
     * to prevent orphaned event lists from leaking Redis memory indefinitely.
     */
    private static final String FLUSH_SCRIPT = """
            local count = redis.call('GETDEL', KEYS[1])
            if not count then
                redis.call('DEL', KEYS[2])
                return {0, {}}
            end
            count = tonumber(count)
            if count <= 0 then
                redis.call('DEL', KEYS[2])
                return {0, {}}
            end
            local events = redis.call('LRANGE', KEYS[2], 0, count - 1)
            redis.call('LTRIM', KEYS[2], count, -1)
            if redis.call('LLEN', KEYS[2]) == 0 then redis.call('DEL', KEYS[2]) end
            return {count, events}
            """;

    private final StringRedisTemplate redisTemplate;
    private final ListingViewRepository listingViewRepository;
    private final ListingViewRedisKeys redisKeys;

    private final DefaultRedisScript<List> flushScript = flushScript();

    /**
     * Scans all buffered view keys in Redis and flushes them to the database.
     *
     * <p>@Transactional is intentionally NOT applied here. The Redis GETDEL
     * inside {@link #flushListing} is irreversible; wrapping the entire scan in
     * one DB transaction means a late failure would roll back all DB inserts
     * while the Redis data is already gone — resulting in data loss. Instead,
     * each listing's flush operates in its own implicit repository transaction.
     */
    @Override
    public void flushAll() {
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(redisKeys.countScanPattern())
                .count(100)
                .build();

        int flushedKeys = 0;
        int flushedViews = 0;

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String countKey = cursor.next();
                UUID listingId = redisKeys.listingIdFromCountKey(countKey);
                if (listingId == null) {
                    log.warn("Skipping malformed Redis view count key: {}", countKey);
                    continue;
                }

                int flushed = flushListing(listingId);
                if (flushed > 0) {
                    flushedKeys++;
                    flushedViews += flushed;
                }
            }
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable — skipping listing-view flush cycle: {}", ex.getMessage());
            return;
        }

        if (flushedKeys > 0) {
            log.info(
                    "Flushed {} buffered view key(s), {} total view(s) to listing_views",
                    flushedKeys,
                    flushedViews);
        }
    }

    private int flushListing(UUID listingId) {
        String countKey = redisKeys.countKey(listingId);
        String eventsKey = redisKeys.eventsKey(listingId);

        List<?> snapshot = redisTemplate.execute(
                flushScript,
                List.of(countKey, eventsKey));

        if (snapshot == null || snapshot.isEmpty()) {
            return 0;
        }

        int count = toInt(snapshot.get(0));
        if (count <= 0) {
            return 0;
        }

        List<String> rawEvents = toEventList(snapshot.size() > 1 ? snapshot.get(1) : null);
        int eventSize = rawEvents.size();

        try {
            int inserted = 0;
            for (String raw : rawEvents) {
                // Guard: a single corrupted JSON payload must not abort the entire batch
                ViewEventPayload event;
                try {
                    event = ViewEventPayload.deserialize(raw);
                } catch (Exception deserializeEx) {
                    log.error("Skipping corrupted view event payload for listing {}: {}",
                            listingId, deserializeEx.getMessage());
                    continue;
                }

                try {
                    listingViewRepository.insertView(
                            listingId,
                            event.userId(),
                            event.ipAddress(),
                            event.userAgent());
                    inserted++;
                } catch (DataIntegrityViolationException ex) {
                    if (isDuplicateKeyViolation(ex)) {
                        log.debug(
                                "Skipped duplicate daily view for listing {} ip {}",
                                listingId,
                                event.ipAddress());
                    } else {
                        // A different constraint violation — log it, don't swallow silently
                        log.error("Unexpected integrity violation inserting view for listing {}: {}",
                                listingId, ex.getMostSpecificCause().getMessage());
                    }
                }
            }

            int remainder = count - eventSize;
            if (remainder > 0) {
                listingViewRepository.insertBufferedViews(listingId, remainder);
                inserted += remainder;
            }

            return inserted;
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to flush views for listing {} after GETDEL; {} view(s) may be lost: {}",
                    listingId,
                    count,
                    ex.getMessage());
            throw ex;
        }
    }

    /**
     * Checks whether the violation is specifically a unique-key (duplicate) constraint
     * (SQL State 23505). This avoids silently swallowing unrelated constraint errors
     * such as NOT NULL violations or foreign key failures.
     */
    private static boolean isDuplicateKeyViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof SQLException sqlEx) {
            return "23505".equals(sqlEx.getSQLState());
        }
        // Fallback string check for non-PSQLException drivers
        String msg = cause != null ? cause.getMessage() : ex.getMessage();
        return msg != null && msg.contains("duplicate key");
    }

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> flushScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(FLUSH_SCRIPT);
        script.setResultType(List.class);
        return script;
    }

    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> toEventList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }
}
