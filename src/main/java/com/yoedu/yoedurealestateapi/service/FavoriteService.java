package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.favorite.AddFavoriteRequest;
import com.yoedu.yoedurealestateapi.dto.favorite.FavoriteResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {

    FavoriteResponse addFavorite(UUID userId, AddFavoriteRequest request);

    void removeFavorite(UUID userId, UUID listingId);

    Page<FavoriteResponse> getMyFavorites(UUID userId, Pageable pageable);
}
