package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.ConflictException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.domain.event.PropertyTypeUpdatedEvent;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import com.yoedu.yoedurealestateapi.repository.PropertyTypeRepository;
import com.yoedu.yoedurealestateapi.service.AdminPropertyTypeService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPropertyTypeServiceImpl implements AdminPropertyTypeService {

    private final PropertyTypeRepository propertyTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    private PropertyTypeResponse toPropertyTypeResponse(PropertyType propertyType) {
        return new PropertyTypeResponse(
            propertyType.getId().toString(),
            propertyType.getName(),
            propertyType.getSlug(),
            propertyType.getIcon(),
            propertyType.getSortOrder()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyTypeResponse> getAllPropertyTypes() {
        return propertyTypeRepository.findAll()
            .stream()
            .map(this::toPropertyTypeResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyTypeResponse getPropertyTypeById(Integer id) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Loại bất động sản không tồn tại"));
        return toPropertyTypeResponse(propertyType);
    }

    @Override
    @Transactional
    public PropertyTypeResponse createPropertyType(UpdatePropertyTypeRequest request) {
        if (propertyTypeRepository.existsBySlugAndIdNot(request.slug(), -1)) {
            throw new ConflictException("Slug đã được sử dụng");
        }
        PropertyType propertyType = new PropertyType();
        propertyType.setName(request.name());
        propertyType.setSlug(request.slug());
        propertyType.setIcon(request.icon());
        if (request.sortOrder() != null) {
            propertyType.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            propertyType.setIsActive(request.isActive());
        }
        PropertyType saved = propertyTypeRepository.save(propertyType);
        return toPropertyTypeResponse(saved);
    }

    @Override
    @Transactional
    public PropertyTypeResponse updatePropertyType(Integer id, UpdatePropertyTypeRequest request) {
        PropertyType propertyType = propertyTypeRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Loại bất động sản không tồn tại"));

        if (propertyTypeRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ConflictException("Slug đã được sử dụng");
        }

        propertyType.setName(request.name());
        propertyType.setSlug(request.slug());
        if (request.icon() != null) {
            propertyType.setIcon(request.icon());
        }
        if (request.sortOrder() != null) {
            propertyType.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            propertyType.setIsActive(request.isActive());
        }

        PropertyType updated = propertyTypeRepository.save(propertyType);

        eventPublisher.publishEvent(new PropertyTypeUpdatedEvent(
            updated.getId(),
            updated.getName(),
            updated.getSlug()
        ));

        return toPropertyTypeResponse(updated);
    }

    @Override
    @Transactional
    public void deletePropertyType(Integer id) {
        if (!propertyTypeRepository.existsById(id)) {
            throw new NotFoundException("Loại bất động sản không tồn tại");
        }
        propertyTypeRepository.deleteById(id);
    }
}
