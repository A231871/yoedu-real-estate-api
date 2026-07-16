package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.service.ListingViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listing")
@RequiredArgsConstructor
@Tag(name = "Listing Views", description = "APIs ghi nhận lượt xem tin đăng (buffer Redis)")
public class ListingViewController {

    private final ListingViewService listingViewService;

    @PostMapping("/{listingId}/views")
    @Operation(summary = "Ghi nhận lượt xem", description = """
            Dedup theo IP/ngày UTC (chống F5 bot), INCR Redis buffer kèm metadata IP/user.
            Cron flush: GET → INSERT listing_views → DECRBY/LTRIM (không GETDEL trước DB).""")
    public ResponseEntity<ApiResponse<Void>> registerView(
            @PathVariable UUID listingId,
            HttpServletRequest request,
            Authentication authentication) {
        UUID userId = authentication != null && authentication.isAuthenticated()
                ? UUID.fromString(authentication.getName())
                : null;

        listingViewService.registerView(
                listingId,
                userId,
                resolveClientIp(request),
                request.getHeader("User-Agent"));

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Đã ghi nhận lượt xem"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
