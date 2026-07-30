package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import java.util.List;

public interface AdminPropertyTypeService {
    List<PropertyTypeResponse> getAllPropertyTypes();
    PropertyTypeResponse getPropertyTypeById(Integer id);
    PropertyTypeResponse createPropertyType(UpdatePropertyTypeRequest request);
    PropertyTypeResponse updatePropertyType(Integer id, UpdatePropertyTypeRequest request);
    void deletePropertyType(Integer id);
}
