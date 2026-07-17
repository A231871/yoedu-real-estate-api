package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.dto.listingview.ViewEventPayload;
import com.yoedu.yoedurealestateapi.redis.ListingViewRedisKeys;
import com.yoedu.yoedurealestateapi.service.ListingViewBufferService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingViewBufferServiceImpl implements ListingViewBufferService {

    private final StringRedisTemplate redisTemplate;
    private final ListingViewRedisKeys redisKeys;

    @Override
    public void bufferView(UUID listingId, UUID userId, String ipAddress, String userAgent) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        LocalDate utcDate = LocalDate.now(ZoneOffset.UTC);
        String dedupKey = redisKeys.dedupKey(listingId, ipAddress, utcDate);
        Boolean firstViewToday = redisTemplate.opsForValue().setIfAbsent(
                dedupKey,
                "1",
                redisKeys.ttlUntilEndOfUtcDay());

        if (!Boolean.TRUE.equals(firstViewToday)) {
            return;
        }

        String countKey = redisKeys.countKey(listingId);
        String eventsKey = redisKeys.eventsKey(listingId);
        String eventPayload = new ViewEventPayload(
                ipAddress,
                userId,
                truncateUserAgent(userAgent)).serialize();

        List<Object> execResult = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes", "NullableProblems"})
            public List<Object> execute(
                    org.springframework.data.redis.core.RedisOperations operations) {
                operations.multi();
                operations.opsForValue().increment(countKey);
                operations.opsForList().rightPush(eventsKey, eventPayload);
                return operations.exec();
            }
        });

        if (execResult == null || execResult.isEmpty()) {
            redisTemplate.delete(dedupKey);
        }
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }
}
