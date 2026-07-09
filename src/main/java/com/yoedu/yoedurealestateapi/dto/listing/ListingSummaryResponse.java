package com.yoedu.yoedurealestateapi.dto.listing;

import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import java.math.BigDecimal;
import java.util.List;

public record ListingSummaryResponse(
    String id,
    String ownerId,
    String agentId,
    String title,
    String slug,
    String description,
    String provinceName,
    List<ListingMediaDto> thumbnails, // 1-3 thumbnails
    BigDecimal area,
    ListingType listingType,
    BigDecimal currentPrice
) {}
