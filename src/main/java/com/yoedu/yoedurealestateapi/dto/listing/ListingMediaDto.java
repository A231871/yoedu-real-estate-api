package com.yoedu.yoedurealestateapi.dto.listing;

import com.yoedu.yoedurealestateapi.domain.enums.MediaType;

public record ListingMediaDto(
    String url,
    String caption,
    MediaType mediaType,
    Integer sortOrder,
    Boolean isPrimary
) {}
