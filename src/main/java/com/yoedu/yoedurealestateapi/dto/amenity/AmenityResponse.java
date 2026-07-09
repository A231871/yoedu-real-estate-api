package com.yoedu.yoedurealestateapi.dto.amenity;

import com.yoedu.yoedurealestateapi.domain.enums.AmenityCategory;

public record AmenityResponse(
    String id,
    String name,
    String slug,
    String icon,
    AmenityCategory category,
    Integer sortOrder
) {}
