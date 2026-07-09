package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.Amenity;
import com.yoedu.yoedurealestateapi.dto.amenity.AmenityResponse;
import com.yoedu.yoedurealestateapi.repository.AmenityRepository;
import com.yoedu.yoedurealestateapi.service.AmenityService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;

    private AmenityResponse toAmenityResponse(Amenity amenity) {
        return new AmenityResponse(
            amenity.getId().toString(),
            amenity.getName(),
            amenity.getSlug(),
            amenity.getIcon(),
            amenity.getCategory(),
            amenity.getSortOrder()
        );
    }

    @Override
    public List<AmenityResponse> getAmenities() {
        return amenityRepository
            .findAll()
            .stream()
            .map(this::toAmenityResponse)
            .collect(Collectors.toList());
    }
}
