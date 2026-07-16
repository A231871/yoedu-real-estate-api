package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.ConflictException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Favorite;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.dto.favorite.AddFavoriteRequest;
import com.yoedu.yoedurealestateapi.dto.favorite.FavoriteResponse;
import com.yoedu.yoedurealestateapi.repository.FavoriteRepository;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.FavoriteService;
import com.yoedu.yoedurealestateapi.service.ListingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingService listingService;

    @Override
    @Transactional
    public FavoriteResponse addFavorite(UUID userId, AddFavoriteRequest request) {
        Listing listing = listingRepository
                .findByIdAndDeletedAtIsNull(request.getListingId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tin đăng hoặc tin đăng đã bị xóa"));

        if (!ListingStatus.APPROVED.equals(listing.getStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể lưu tin đăng đã được phê duyệt vào danh sách yêu thích");
        }

        if (favoriteRepository.existsByUser_IdAndListing_Id(userId, listing.getId())) {
            throw new ConflictException("Tin đăng này đã có trong danh sách yêu thích");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setListing(listing);
        favorite.setNote(request.getNote());

        Favorite saved = favoriteRepository.save(favorite);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID userId, UUID listingId) {
        int deleted = favoriteRepository.deleteByUser_IdAndListing_Id(userId, listingId);
        if (deleted == 0) {
            throw new NotFoundException("Không tìm thấy tin đăng trong danh sách yêu thích");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getMyFavorites(UUID userId, Pageable pageable) {
        return favoriteRepository
                .findByUser_IdAndListing_DeletedAtIsNullOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDto);
    }

    private FavoriteResponse toDto(Favorite favorite) {
        FavoriteResponse dto = new FavoriteResponse();
        dto.setId(favorite.getId());
        dto.setListingId(favorite.getListing().getId());
        dto.setNote(favorite.getNote());
        dto.setFavoritedAt(favorite.getCreatedAt());
        dto.setListing(listingService.toListingSummary(favorite.getListing()));
        return dto;
    }
}
