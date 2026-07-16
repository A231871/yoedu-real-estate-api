package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import java.util.List;
import java.util.Optional;

public interface ListingService {
    List<ListingSummaryResponse> getListingSummaries();

    ListingSummaryResponse toListingSummary(Listing listing);

    Optional<ListingDetailResponse> getListingDetail(String id);

    void createListing(ListingUpsertRequest request);

    void updateListing(String id, ListingUpsertRequest request);

    void deleteListing(String id);
}
