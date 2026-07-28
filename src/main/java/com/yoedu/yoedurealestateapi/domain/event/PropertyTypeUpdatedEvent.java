package com.yoedu.yoedurealestateapi.domain.event;

public record PropertyTypeUpdatedEvent(
    Integer id,
    String name,
    String slug
) {}
