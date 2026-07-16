package com.yoedu.yoedurealestateapi.scheduler;

import com.yoedu.yoedurealestateapi.repository.ListingViewRepository;
import com.yoedu.yoedurealestateapi.service.listingview.ListingViewRedisKeys;
import com.yoedu.yoedurealestateapi.service.listingview.ViewEventPayload;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListingViewFlushCronJob {

    private final StringRedisTemplate redisTemplate;
    private final ListingViewRepository listingViewRepository;
    private final ListingViewRedisKeys redisKeys;

    /**
     * Flushes buffered counts: GET first, persist to DB, then remove from Redis only on success.
     * Avoids data loss if DB insert fails (unlike GETDEL-before-insert).
     */
    @Scheduled(cron = "${app.listing-views.cron.flush:0 */5 * * * *}")
    @SchedulerLock(
            name = "listingViewFlush",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT4M")
    @Transactional
    public void flushBufferedViews() {
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

        String countValue = redisTemplate.opsForValue().get(countKey);
        if (countValue == null || countValue.isBlank()) {
            return 0;
        }

        int count;
        try {
            count = Integer.parseInt(countValue);
        } catch (NumberFormatException ex) {
            log.warn("Invalid view count for listing {}: {}", listingId, countValue);
            return 0;
        }

        if (count <= 0) {
            redisTemplate.delete(countKey);
            redisTemplate.delete(eventsKey);
            return 0;
        }

        List<String> rawEvents = redisTemplate.opsForList().range(eventsKey, 0, count - 1L);
        int eventSize = rawEvents != null ? rawEvents.size() : 0;

        try {
            int inserted = 0;
            if (rawEvents != null) {
                for (String raw : rawEvents) {
                    ViewEventPayload event = ViewEventPayload.deserialize(raw);
                    try {
                        listingViewRepository.insertView(
                                listingId,
                                event.userId(),
                                event.ipAddress(),
                                event.userAgent());
                        inserted++;
                    } catch (DataIntegrityViolationException ex) {
                        log.debug(
                                "Skipped duplicate daily view for listing {} ip {}",
                                listingId,
                                event.ipAddress());
                    }
                }
            }

            int remainder = count - eventSize;
            if (remainder > 0) {
                listingViewRepository.insertBufferedViews(listingId, remainder);
                inserted += remainder;
            }

            removeFlushedFromRedis(countKey, eventsKey, count);
            return inserted;
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to flush views for listing {}; Redis buffer retained: {}",
                    listingId,
                    ex.getMessage());
            throw ex;
        }
    }

    private void removeFlushedFromRedis(String countKey, String eventsKey, int flushedCount) {
        Long remainingCount = redisTemplate.opsForValue().decrement(countKey, flushedCount);
        redisTemplate.opsForList().trim(eventsKey, flushedCount, -1);

        if (remainingCount == null || remainingCount <= 0) {
            redisTemplate.delete(countKey);
        }

        Long eventsRemaining = redisTemplate.opsForList().size(eventsKey);
        if (eventsRemaining == null || eventsRemaining == 0) {
            redisTemplate.delete(eventsKey);
        }
    }
}
