package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import com.yoedu.yoedurealestateapi.service.ListingService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listing")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ListingSummaryResponse>>> getListings(
        @ParameterObject
        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable,
        @RequestParam ListingType listingType,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Integer minBedrooms,
        @RequestParam(required = false) Integer maxBedrooms,
        @RequestParam(required = false) Integer minBathrooms,
        @RequestParam(required = false) Integer maxBathrooms,
        @RequestParam(required = false) BigDecimal minArea,
        @RequestParam(required = false) BigDecimal maxArea,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String provinceCode,
        @RequestParam(required = false) String wardCode,
        @RequestParam(required = false) List<Integer> amenityIds
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(
                listingService.getListingSummaries(
                    pageable,
                    listingType,
                    minPrice,
                    maxPrice,
                    minBedrooms,
                    maxBedrooms,
                    minBathrooms,
                    maxBathrooms,
                    minArea,
                    maxArea,
                    title,
                    provinceCode,
                    wardCode,
                    amenityIds
                )
            )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingDetailResponse>> getListingDetail(
        @PathVariable String id
    ) {
        ListingDetailResponse listingDetailResponse = listingService
            .getListingDetail(id)
            .orElseThrow(() -> new NotFoundException("Listing not found"));

        return ResponseEntity.ok(ApiResponse.success(listingDetailResponse));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createListing(
        @Valid @RequestBody ListingUpsertRequest request
    ) {
        listingService.createListing(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Created new listing"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateListing(
        @PathVariable String id,
        @Valid @RequestBody ListingUpsertRequest request
    ) {
        listingService.updateListing(id, request);
        return ResponseEntity.ok(ApiResponse.success("Updated listing"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteListing(@PathVariable String id) {
        listingService.deleteListing(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted listing"));
    }
}
