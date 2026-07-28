package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.common.exception.ConflictException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.domain.event.PropertyTypeUpdatedEvent;
import com.yoedu.yoedurealestateapi.dto.property_type.PropertyTypeResponse;
import com.yoedu.yoedurealestateapi.dto.property_type.UpdatePropertyTypeRequest;
import com.yoedu.yoedurealestateapi.repository.PropertyTypeRepository;
import com.yoedu.yoedurealestateapi.service.impl.AdminPropertyTypeServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminPropertyTypeServiceTest {

    @Mock
    private PropertyTypeRepository propertyTypeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminPropertyTypeServiceImpl adminPropertyTypeService;

    private PropertyType propertyType;

    @BeforeEach
    void setUp() {
        propertyType = new PropertyType();
        propertyType.setId(1);
        propertyType.setName("Căn hộ");
        propertyType.setSlug("can-ho");
        propertyType.setIcon("building");
        propertyType.setSortOrder(1);
        propertyType.setIsActive(true);
    }

    @Test
    void updatePropertyType_Success() {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest(
            "Căn hộ cao cấp", "can-ho-cao-cap", "building-star", 2, true
        );

        when(propertyTypeRepository.findById(1)).thenReturn(Optional.of(propertyType));
        when(propertyTypeRepository.existsBySlugAndIdNot("can-ho-cao-cap", 1)).thenReturn(false);
        when(propertyTypeRepository.save(any(PropertyType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PropertyTypeResponse response = adminPropertyTypeService.updatePropertyType(1, request);

        assertNotNull(response);
        assertEquals("1", response.id());
        assertEquals("Căn hộ cao cấp", response.name());
        assertEquals("can-ho-cao-cap", response.slug());

        ArgumentCaptor<PropertyTypeUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(PropertyTypeUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PropertyTypeUpdatedEvent event = eventCaptor.getValue();
        assertEquals(1, event.id());
        assertEquals("Căn hộ cao cấp", event.name());
        assertEquals("can-ho-cao-cap", event.slug());
    }

    @Test
    void updatePropertyType_NotFound_ThrowsException() {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("N/A", "na", null, null, null);
        when(propertyTypeRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminPropertyTypeService.updatePropertyType(99, request));
    }

    @Test
    void updatePropertyType_DuplicateSlug_ThrowsException() {
        UpdatePropertyTypeRequest request = new UpdatePropertyTypeRequest("House", "duplicate-slug", null, null, null);
        when(propertyTypeRepository.findById(1)).thenReturn(Optional.of(propertyType));
        when(propertyTypeRepository.existsBySlugAndIdNot("duplicate-slug", 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> adminPropertyTypeService.updatePropertyType(1, request));
    }

    @Test
    void getAllPropertyTypes_Success() {
        when(propertyTypeRepository.findAll()).thenReturn(List.of(propertyType));

        List<PropertyTypeResponse> list = adminPropertyTypeService.getAllPropertyTypes();

        assertEquals(1, list.size());
        assertEquals("Căn hộ", list.get(0).name());
    }

    @Test
    void deletePropertyType_Success() {
        when(propertyTypeRepository.existsById(1)).thenReturn(true);

        adminPropertyTypeService.deletePropertyType(1);

        verify(propertyTypeRepository).deleteById(1);
    }
}
