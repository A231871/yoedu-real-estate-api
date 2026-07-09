package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import com.yoedu.yoedurealestateapi.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listing")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public ApiResponse<List<ListingSummaryResponse>> getListings() {
        return ApiResponse.success(listingService.getListingSummaries());
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
