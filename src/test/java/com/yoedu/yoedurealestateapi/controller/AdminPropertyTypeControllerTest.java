package com.yoedu.yoedurealestateapi.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoedu.yoedurealestateapi.common.exception.ConflictException;
import com.yoedu.yoedurealestateapi.common.exception.GlobalExceptionHandler;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import com.yoedu.yoedurealestateapi.service.AdminPropertyTypeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminPropertyTypeControllerTest {

    private MockMvc mockMvc;
    private AdminPropertyTypeService adminPropertyTypeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adminPropertyTypeService = mock(AdminPropertyTypeService.class);
        AdminPropertyTypeController adminPropertyTypeController = new AdminPropertyTypeController(adminPropertyTypeService);
        mockMvc = MockMvcBuilders.standaloneSetup(adminPropertyTypeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllPropertyTypes_Returns200() throws Exception {
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Nhà trọ", "nha-tro", "home", 1, true);
        when(adminPropertyTypeService.getAllPropertyTypes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/property-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].name", is("Nhà trọ")))
                .andExpect(jsonPath("$.data[0].isActive", is(true)));
    }

    @Test
    void getPropertyTypeById_Returns200() throws Exception {
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Nhà trọ", "nha-tro", "home", 1, true);
        when(adminPropertyTypeService.getPropertyTypeById(1)).thenReturn(dto);

        mockMvc.perform(get("/admin/property-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.slug", is("nha-tro")));
    }

    @Test
    void getPropertyTypeById_NotFound_Returns404() throws Exception {
        when(adminPropertyTypeService.getPropertyTypeById(99))
                .thenThrow(new NotFoundException("Loại bất động sản không tồn tại"));

        mockMvc.perform(get("/admin/property-types/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void createPropertyType_ValidRequest_Returns201Created() throws Exception {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("Căn hộ", "can-ho", "building", 2, true);
        PropertyTypeResponse dto = new PropertyTypeResponse("2", "Căn hộ", "can-ho", "building", 2, true);
        when(adminPropertyTypeService.createPropertyType(any())).thenReturn(dto);

        mockMvc.perform(post("/admin/property-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Căn hộ")));
    }

    @Test
    void createPropertyType_InvalidBlankName_Returns400BadRequest() throws Exception {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("", "valid-slug", null, null, null);

        mockMvc.perform(post("/admin/property-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void updatePropertyType_ValidRequest_Returns200OK() throws Exception {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("Căn hộ cao cấp", "can-ho-cao-cap", "building-star", 2, true);
        PropertyTypeResponse dto = new PropertyTypeResponse("1", "Căn hộ cao cấp", "can-ho-cao-cap", "building-star", 2, true);
        when(adminPropertyTypeService.updatePropertyType(eq(1), any())).thenReturn(dto);

        mockMvc.perform(put("/admin/property-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.slug", is("can-ho-cao-cap")));
    }

    @Test
    void updatePropertyType_DuplicateSlug_Returns409Conflict() throws Exception {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("Căn hộ", "duplicate-slug", null, null, null);
        when(adminPropertyTypeService.updatePropertyType(eq(1), any()))
                .thenThrow(new ConflictException("Slug đã được sử dụng"));

        mockMvc.perform(put("/admin/property-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void deletePropertyType_Returns200OK() throws Exception {
        mockMvc.perform(delete("/admin/property-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(adminPropertyTypeService).deletePropertyType(1);
    }

    @Test
    void deletePropertyType_InUse_Returns409Conflict() throws Exception {
        doThrow(new ConflictException("Không thể xóa loại bất động sản đang được sử dụng bởi các bài đăng"))
                .when(adminPropertyTypeService).deletePropertyType(1);

        mockMvc.perform(delete("/admin/property-types/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
