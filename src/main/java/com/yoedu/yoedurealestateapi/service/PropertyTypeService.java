package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import java.util.List;

public interface PropertyTypeService {
    List<PropertyTypeResponse> getPropertyTypes();
}
