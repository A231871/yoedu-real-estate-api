package com.yoedu.yoedurealestateapi.dto.favorite;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddFavoriteRequest {

    @NotNull(message = "Listing id must not be null")
    private UUID listingId;

    @Size(max = 300, message = "Note must not exceed 300 characters")
    private String note;
}
