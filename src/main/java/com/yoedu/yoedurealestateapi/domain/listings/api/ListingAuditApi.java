package com.yoedu.yoedurealestateapi.domain.listings.api;

import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import java.util.List;
import java.util.UUID;

public interface ListingAuditApi {
    List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId);
}
