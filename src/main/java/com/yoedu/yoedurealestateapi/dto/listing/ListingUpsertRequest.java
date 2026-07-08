package com.yoedu.yoedurealestateapi.dto.listing;

import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingUpsertRequest {

    @NotNull
    private String ownerId;

    private String agentId;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String address;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal area;

    @Positive
    private Integer bedrooms;

    @Positive
    private Integer bathrooms;

    @Positive
    private Integer floors;

    @NotNull
    private ListingType listingType;

    @NotNull
    private String propertyTypeId;

    @NotBlank
    private String wardCode;

    private Set<String> amenityIds = new HashSet<>();

    private Set<ListingMediaDto> listingMediaDtos = new HashSet<>();

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;
}
