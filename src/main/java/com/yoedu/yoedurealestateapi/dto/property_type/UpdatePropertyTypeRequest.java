package com.yoedu.yoedurealestateapi.dto.property_type;

import jakarta.validation.constraints.NotBlank;

public record UpdatePropertyTypeRequest(
    @NotBlank(message = "Tên loại bất động sản không được để trống")
    String name,

    @NotBlank(message = "Slug không được để trống")
    String slug,

    String icon,
    Integer sortOrder,
    Boolean isActive
) {}
