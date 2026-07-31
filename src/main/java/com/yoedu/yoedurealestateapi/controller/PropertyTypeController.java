package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.service.PropertyTypeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/property-type")
@RequiredArgsConstructor
public class PropertyTypeController {

    private final PropertyTypeService propertyTypeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> getPropertyTypes() {
        return ResponseEntity.ok(ApiResponse.success(propertyTypeService.getPropertyTypes()));
    }
}
