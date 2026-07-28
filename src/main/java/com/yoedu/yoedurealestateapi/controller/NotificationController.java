package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.notification.NotificationResponse;
import com.yoedu.yoedurealestateapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "APIs quản lý hộp thư và lịch sử thông báo")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy danh sách thông báo của người dùng hiện tại có phân trang")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<NotificationResponse> page = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", page));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Lấy số lượng thông báo chưa đọc", description = "Lấy tổng số thông báo chưa đọc để hiển thị badge")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo là đã đọc", description = "Cập nhật trạng thái một thông báo cụ thể sang đã đọc")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationResponse response = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu thông báo đã đọc thành công", response));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc", description = "Cập nhật trạng thái tất cả thông báo của người dùng hiện tại sang đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Đánh dấu tất cả thông báo đã đọc thành công"));
    }
}
