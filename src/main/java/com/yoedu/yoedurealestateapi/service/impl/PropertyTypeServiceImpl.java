package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.repository.PropertyTypeRepository;
import com.yoedu.yoedurealestateapi.service.PropertyTypeService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyTypeServiceImpl implements PropertyTypeService {

    private final PropertyTypeRepository propertyTypeRepository;

    private PropertyTypeResponse toPropertyTypeResponse(PropertyType propertyType) {
        return new PropertyTypeResponse(
            propertyType.getId().toString(),
            propertyType.getName(),
            propertyType.getSlug(),
            propertyType.getIcon(),
            propertyType.getSortOrder(),
            propertyType.getIsActive()
        );
    }

    @Override
    @Cacheable(value = "propertyTypes", key = "'all'")
    public List<PropertyTypeResponse> getPropertyTypes() {
        return propertyTypeRepository
            .findAllByOrderBySortOrderAsc()
            .stream()
            .map(this::toPropertyTypeResponse)
            .collect(Collectors.toList());
    }
}
