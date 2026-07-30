package com.yoedu.yoedurealestateapi.dto.property_type;

public record PropertyTypeResponse(
    String id,
    String name,
    String slug,
    String icon,
    Integer sortOrder,
    Boolean isActive
) {}
