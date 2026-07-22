package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.service.ListingViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" + // IPv4 (0-255)
            "|([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}" +                                                // IPv6 full
            "|::1" +                                                                                  // IPv6 loopback
            "|([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4})$"                                              // IPv6 compressed
    );
    private static final String FALLBACK_IP = "0.0.0.0";

    private final ListingViewService listingViewService;

    @PostMapping("/{listingId}/views")
    @Operation(summary = "Ghi nhận lượt xem", description = """
            Dedup theo IP/ngày UTC (chống F5 bot), INCR Redis buffer kèm metadata IP/user.
            Cron flush: Lua GETDEL count + snapshot events → INSERT listing_views.""")
    public ResponseEntity<ApiResponse<Void>> registerView(
            @PathVariable UUID listingId,
            HttpServletRequest request,
            Authentication authentication) {
        UUID userId = null;
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !"anonymousUser".equals(authentication.getPrincipal())
                && !"anonymousUser".equals(authentication.getName())) {
            try {
                userId = UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException ignored) {
                // Anonymous or non-UUID principal
            }
        }

        listingViewService.registerView(
                listingId,
                userId,
                resolveClientIp(request),
                request.getHeader("User-Agent"));

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Đã ghi nhận lượt xem", null));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            for (String ip : forwarded.split(",")) {
                String trimmed = ip.trim();
                if (!trimmed.isBlank() && !"unknown".equalsIgnoreCase(trimmed)) {
                    return sanitizeIp(trimmed);
                }
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return sanitizeIp(realIp.trim());
        }

        return sanitizeIp(request.getRemoteAddr());
    }

    private String sanitizeIp(String ip) {
        if (ip == null || !IP_PATTERN.matcher(ip).matches()) {
            return FALLBACK_IP;
        }
        return ip;
    }
}

