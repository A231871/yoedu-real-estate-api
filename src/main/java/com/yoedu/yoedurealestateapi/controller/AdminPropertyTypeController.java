package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import com.yoedu.yoedurealestateapi.service.AdminPropertyTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/property-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Property Types", description = "Admin management endpoints for property types")
public class AdminPropertyTypeController {

    private final AdminPropertyTypeService adminPropertyTypeService;

    @GetMapping
    @Operation(summary = "Get all property types", description = "Retrieves all property types for administration")
    public ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> getAllPropertyTypes() {
        List<PropertyTypeResponse> result = adminPropertyTypeService.getAllPropertyTypes();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách loại bất động sản thành công", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property type by ID", description = "Retrieves a specific property type by its ID")
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> getPropertyTypeById(@PathVariable Integer id) {
        PropertyTypeResponse result = adminPropertyTypeService.getPropertyTypeById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin loại bất động sản thành công", result));
    }

    @PostMapping
    @Operation(summary = "Create property type", description = "Creates a new property type")
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> createPropertyType(
            @Valid @RequestBody UpdatePropertyTypeRequest request) {
        PropertyTypeResponse result = adminPropertyTypeService.createPropertyType(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo loại bất động sản thành công", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update property type", description = "Updates an existing property type and publishes PropertyTypeUpdatedEvent")
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> updatePropertyType(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePropertyTypeRequest request) {
        PropertyTypeResponse result = adminPropertyTypeService.updatePropertyType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật loại bất động sản thành công", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete property type", description = "Deletes a property type by ID")
    public ResponseEntity<ApiResponse<Void>> deletePropertyType(@PathVariable Integer id) {
        adminPropertyTypeService.deletePropertyType(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa loại bất động sản thành công", null));
    }
}
