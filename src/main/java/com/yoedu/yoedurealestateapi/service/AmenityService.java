package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.amenity.AmenityResponse;
import java.util.List;

public interface AmenityService {
    List<AmenityResponse> getAmenities();
}
