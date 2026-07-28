package com.yoedu.yoedurealestateapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import com.yoedu.yoedurealestateapi.service.AdminPropertyTypeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AdminPropertyTypeControllerTest {

    private AdminPropertyTypeService adminPropertyTypeService;
    private AdminPropertyTypeController adminPropertyTypeController;

    @BeforeEach
    void setUp() {
        adminPropertyTypeService = mock(AdminPropertyTypeService.class);
        adminPropertyTypeController = new AdminPropertyTypeController(adminPropertyTypeService);
    }

    @Test
    void getAllPropertyTypes_Success() {
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Nhà trọ", "nha-tro", "home", 1);
        when(adminPropertyTypeService.getAllPropertyTypes()).thenReturn(List.of(dto));

        ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> response =
            adminPropertyTypeController.getAllPropertyTypes();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
        assertEquals(1, response.getBody().data().size());
        assertEquals("Nhà trọ", response.getBody().data().get(0).name());
    }

    @Test
    void getPropertyTypeById_Success() {
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Nhà trọ", "nha-tro", "home", 1);
        when(adminPropertyTypeService.getPropertyTypeById(1)).thenReturn(dto);

        ResponseEntity<ApiResponse<PropertyTypeResponse>> response =
            adminPropertyTypeController.getPropertyTypeById(1);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("nha-tro", response.getBody().data().slug());
    }

    @Test
    void createPropertyType_Success() {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("Căn hộ", "can-ho", "building", 2, true);
        PropertyTypeResponse dto = new PropertyTypeResponse("2", "Căn hộ", "can-ho", "building", 2);
        when(adminPropertyTypeService.createPropertyType(any())).thenReturn(dto);

        ResponseEntity<ApiResponse<PropertyTypeResponse>> response =
            adminPropertyTypeController.createPropertyType(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Căn hộ", response.getBody().data().name());
    }

    @Test
    void updatePropertyType_Success() {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("Căn hộ cao cấp", "can-ho-cao-cap", "building-star", 2, true);
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Căn hộ cao cấp", "can-ho-cao-cap", "building-star", 2);
        when(adminPropertyTypeService.updatePropertyType(eq(1), any())).thenReturn(dto);

        ResponseEntity<ApiResponse<PropertyTypeResponse>> response =
            adminPropertyTypeController.updatePropertyType(1, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("can-ho-cao-cap", response.getBody().data().slug());
    }

    @Test
    void deletePropertyType_Success() {
        ResponseEntity<ApiResponse<Void>> response = adminPropertyTypeController.deletePropertyType(1);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().success());
        verify(adminPropertyTypeService).deletePropertyType(1);
    }
}
