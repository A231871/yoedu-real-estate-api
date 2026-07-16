package com.yoedu.yoedurealestateapi.dto.favorite;

import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteResponse {

    private UUID id;
    private UUID listingId;
    private String note;
    private Instant favoritedAt;
    private ListingSummaryResponse listing;
}
