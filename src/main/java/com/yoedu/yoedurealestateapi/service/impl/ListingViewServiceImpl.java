package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.service.ListingViewService;
import com.yoedu.yoedurealestateapi.service.listingview.ListingViewRedisKeys;
import com.yoedu.yoedurealestateapi.service.listingview.ViewEventPayload;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingViewServiceImpl implements ListingViewService {

    private final StringRedisTemplate redisTemplate;
    private final ListingRepository listingRepository;
    private final ListingViewRedisKeys redisKeys;

    @Value("${app.listing-views.listing-cache-ttl-minutes:15}")
    private long listingCacheTtlMinutes;

    @Override
    public void registerView(UUID listingId, UUID userId, String ipAddress, String userAgent) {
        ensureListingApprovedCached(listingId);

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

    private void ensureListingApprovedCached(UUID listingId) {
        String cacheKey = redisKeys.listingApprovedKey(listingId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            return;
        }

        var listing = listingRepository.findByIdAndDeletedAtIsNull(listingId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tin đăng hoặc tin đăng đã bị xóa"));

        if (!ListingStatus.APPROVED.equals(listing.getStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể ghi nhận lượt xem cho tin đăng đã được phê duyệt");
        }

        redisTemplate.opsForValue().set(
                cacheKey,
                "1",
                Duration.ofMinutes(listingCacheTtlMinutes));
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }
}
