package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.exception.ApiResponse;
import com.yoedu.yoedurealestateapi.service.ViewingScheduleService;
import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.ViewingScheduleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/viewing-schedules")
@RequiredArgsConstructor
public class ViewingScheduleController {

    private final ViewingScheduleService viewingScheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> createSchedule(
            @Valid @RequestBody UpsertViewingScheduleRequest request,
            Authentication authentication) {
        UUID clientId = UUID.fromString(authentication.getName());
        ViewingScheduleResponse response = viewingScheduleService.createSchedule(request, clientId);
        return ResponseEntity.ok(ApiResponse.ok("Tạo lịch hẹn xem nhà thành công", response));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("@viewingScheduleSecurity.isListingOwner(#id)")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> confirmSchedule(@PathVariable UUID id) {
        ViewingScheduleResponse response = viewingScheduleService.confirmSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận lịch hẹn xem nhà thành công", response));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@viewingScheduleSecurity.isListingOwner(#id)")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> cancelSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertViewingScheduleRequest cancelRequest,
            Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        // Sử dụng cancelRequest.getReason() từ DTO mới
        ViewingScheduleResponse response = viewingScheduleService.cancelSchedule(id, cancelRequest.getReason(), actorId);
        return ResponseEntity.ok(ApiResponse.ok("Hủy lịch hẹn xem nhà thành công", response));
    }
}