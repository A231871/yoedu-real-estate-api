package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {
    Page<ListingSummaryResponse> getListingSummaries(
        Pageable pageable,
        ListingType listingType,

        // Price range
        BigDecimal minPrice,
        BigDecimal maxPrice,
        // Bedrooms range
        Integer minBedRooms,
        Integer maxBedroom,
        // Bathrooms range
        Integer minBathrooms,
        Integer maxBathrooms,
        // Area range
        BigDecimal minArea,
        BigDecimal maxArea,

        String title,
        String provinceCode,
        String wardCode,
        List<Integer> amenityIds
    );

    ListingSummaryResponse toListingSummary(Listing listing);

    Optional<ListingDetailResponse> getListingDetail(String id);

    void createListing(ListingUpsertRequest request);

    void updateListing(String id, ListingUpsertRequest request);

    void deleteListing(String id);
}
