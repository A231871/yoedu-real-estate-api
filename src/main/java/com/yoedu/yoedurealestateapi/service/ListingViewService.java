package com.yoedu.yoedurealestateapi.service;

import java.util.UUID;

public interface ListingViewService {

    void registerView(UUID listingId, UUID userId, String ipAddress, String userAgent);
}
