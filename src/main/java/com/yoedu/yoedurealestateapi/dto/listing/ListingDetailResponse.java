package com.yoedu.yoedurealestateapi.dto.listing;

import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import java.math.BigDecimal;
import java.util.List;

public record ListingDetailResponse(
    String id,
    String ownerId,
    String agentId,

    String title,
    String slug,
    String description,
    String address,

    BigDecimal area,
    Integer bedrooms,
    Integer bathrooms,
    Integer floors,

    ListingStatus status,
    ListingType listingType,

    String propertyType,
    String province,
    String ward,

    BigDecimal currentPrice,

    List<ListingMediaDto> listingMediaDtos,

    List<String> amenities
) {}
