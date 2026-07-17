package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import com.yoedu.yoedurealestateapi.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ApiResponse<Page<ListingSummaryResponse>> getListings(
        @ParameterObject
        @PageableDefault(
            size = 20,
            sort = "createdAt",
            direction = Sort.Direction.DESC
        ) Pageable pageable,
        @RequestParam ListingType listingType
    ) {
        return ApiResponse.success(listingService.getListingSummaries(pageable, listingType));
    }

    @GetMapping("/{id}")
    public ApiResponse<ListingDetailResponse> getListingDetail(
        @PathVariable String id
    ) {
        ListingDetailResponse listingDetailResponse = listingService
            .getListingDetail(id)
            .orElseThrow(() -> new NotFoundException("Listing not found"));

        return ApiResponse.success(listingDetailResponse);
    }

    @PostMapping
    public ApiResponse<String> createListing(
        @Valid @RequestBody ListingUpsertRequest request
    ) {
        listingService.createListing(request);
        return ApiResponse.success("Created new listing");
    }

    @PutMapping("/{id}")
    public ApiResponse<String> updateListing(
        @PathVariable String id,
        @Valid @RequestBody ListingUpsertRequest request
    ) {
        listingService.updateListing(id, request);
        return ApiResponse.success("Updated listing");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteListing(@PathVariable String id) {
        listingService.deleteListing(id);
        return ApiResponse.success("Deleted listing");
    }
}
