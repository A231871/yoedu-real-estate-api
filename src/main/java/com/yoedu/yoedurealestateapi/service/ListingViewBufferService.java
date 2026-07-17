package com.yoedu.yoedurealestateapi.service;

import java.util.UUID;

public interface ListingViewBufferService {

    void bufferView(UUID listingId, UUID userId, String ipAddress, String userAgent);
}
