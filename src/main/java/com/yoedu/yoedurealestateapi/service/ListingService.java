package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListingService {
    Page<ListingSummaryResponse> getListingSummaries(Pageable pageable, ListingType listingType);

    Optional<ListingDetailResponse> getListingDetail(String id);

    void createListing(ListingUpsertRequest request);

    void updateListing(String id, ListingUpsertRequest request);

    void deleteListing(String id);
}
