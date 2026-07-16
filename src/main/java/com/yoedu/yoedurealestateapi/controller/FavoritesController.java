package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.favorite.AddFavoriteRequest;
import com.yoedu.yoedurealestateapi.dto.favorite.FavoriteResponse;
import com.yoedu.yoedurealestateapi.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "APIs quản lý danh sách yêu thích")
public class FavoritesController {

    private final FavoriteService favoriteService;

    @PostMapping
    @Operation(summary = "Thêm tin đăng vào yêu thích")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        FavoriteResponse saved = favoriteService.addFavorite(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã thêm vào danh sách yêu thích", saved));
    }

    @DeleteMapping("/{listingId}")
    @Operation(summary = "Xóa tin đăng khỏi yêu thích")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable UUID listingId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        favoriteService.removeFavorite(userId, listingId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi danh sách yêu thích"));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách yêu thích của user hiện tại")
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> getMyFavorites(
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<FavoriteResponse> page = favoriteService.getMyFavorites(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu thích thành công", page));
    }
}
