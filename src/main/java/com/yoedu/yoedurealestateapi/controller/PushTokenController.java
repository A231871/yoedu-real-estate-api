package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.notification.PushTokenResponse;
import com.yoedu.yoedurealestateapi.dto.notification.UpsertPushTokenRequest;
import com.yoedu.yoedurealestateapi.service.PushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/tokens")
@RequiredArgsConstructor
@Tag(name = "Push Notifications", description = "APIs quản lý Push Notification Tokens")
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @PostMapping
    @Operation(summary = "Lưu hoặc cập nhật Push Token", description = "Đăng ký hoặc cập nhật FCM/WEB push token cho thiết bị của người dùng hiện tại")
    public ResponseEntity<ApiResponse<PushTokenResponse>> upsertPushToken(
            @Valid @RequestBody UpsertPushTokenRequest request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        PushTokenResponse response = pushTokenService.upsertPushToken(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu token thiết bị thành công", response));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Hủy Push Token theo deviceId", description = "Vô hiệu hóa push token của thiết bị khi người dùng đăng xuất")
    public ResponseEntity<ApiResponse<Void>> deactivatePushToken(
            @PathVariable String deviceId,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        pushTokenService.deactivateDeviceToken(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.successMessage("Hủy token thiết bị thành công"));
    }
}
