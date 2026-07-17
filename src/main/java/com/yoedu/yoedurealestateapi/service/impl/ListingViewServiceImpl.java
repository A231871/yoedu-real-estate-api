package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.redis.ListingViewRedisKeys;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.service.ListingViewBufferService;
import com.yoedu.yoedurealestateapi.service.ListingViewService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingViewServiceImpl implements ListingViewService {

    private final StringRedisTemplate redisTemplate;
    private final ListingRepository listingRepository;
    private final ListingViewRedisKeys redisKeys;
    private final ListingViewBufferService bufferService;

    @Value("${app.listing-views.listing-cache-ttl-minutes:15}")
    private long listingCacheTtlMinutes;

    @Override
    public void registerView(UUID listingId, UUID userId, String ipAddress, String userAgent) {
        ensureListingApprovedCached(listingId);
        bufferService.bufferView(listingId, userId, ipAddress, userAgent);
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
}
