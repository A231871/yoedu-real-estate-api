package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.exception.ApiResponse;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.ViewingScheduleResponse;
import com.yoedu.yoedurealestateapi.service.ViewingScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD controller cho Viewing Schedules.
 * Controller nhận entity từ Service rồi dùng ModelMapper map sang DTO trước khi trả về.
 */
@RestController
@RequestMapping("/api/viewing-schedules")
@RequiredArgsConstructor
@Tag(name = "Viewing Schedules", description = "APIs quản lý lịch hẹn xem nhà")
public class ViewingScheduleController {

    private final ViewingScheduleService viewingScheduleService;
    private final ModelMapper modelMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renter đặt lịch hẹn xem nhà cho một listing đã được duyệt.
     */
    @PostMapping
    @Operation(summary = "Đặt lịch hẹn xem nhà",
        description = "Renter tạo yêu cầu xem nhà cho listing có trạng thái APPROVED")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> createSchedule(
            @Valid @RequestBody UpsertViewingScheduleRequest request,
            Authentication authentication) {
        UUID clientId = UUID.fromString(authentication.getName());
        ViewingSchedule saved = viewingScheduleService.createSchedule(request, clientId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Tạo lịch hẹn xem nhà thành công", toDto(saved)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy chi tiết một lịch hẹn theo ID (cả host lẫn client đều xem được).
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết lịch hẹn", description = "Xem thông tin một lịch hẹn cụ thể")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> getById(
            @PathVariable UUID id) {
        ViewingSchedule schedule = viewingScheduleService.getScheduleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Lấy thông tin lịch hẹn thành công", toDto(schedule)));
    }

    /**
     * Lấy danh sách lịch hẹn của các listing đang được quản lý, có thể lọc theo status.
     * Ví dụ: GET /api/viewing-schedules/managed?status=PENDING&status=CONFIRMED&page=0&size=10
     */
    @GetMapping("/managed")
    @Operation(summary = "Lấy lịch hẹn quản lý",
        description = "Xem danh sách lịch hẹn của các listing đang được quản lý, lọc theo status tuỳ chọn")
    public ResponseEntity<ApiResponse<Page<ViewingScheduleResponse>>> getManagedSchedules(
            @RequestParam(required = false) List<String> status,
            Pageable pageable,
            Authentication authentication) {
        UUID hostId = UUID.fromString(authentication.getName());
        Page<ViewingScheduleResponse> page = viewingScheduleService
            .getHostSchedules(hostId, status, pageable)
            .map(this::toDto);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách lịch hẹn thành công", page));
    }

    /**
     * Xem danh sách lịch hẹn đã đặt, có thể lọc theo status.
     * Ví dụ: GET /api/viewing-schedules/requested?status=CONFIRMED&page=0&size=10
     */
    @GetMapping("/requested")
    @Operation(summary = "Lấy lịch hẹn đã yêu cầu",
        description = "Xem danh sách lịch hẹn đã yêu cầu, lọc theo status tuỳ chọn")
    public ResponseEntity<ApiResponse<Page<ViewingScheduleResponse>>> getRequestedSchedules(
            @RequestParam(required = false) List<String> status,
            Pageable pageable,
            Authentication authentication) {
        UUID clientId = UUID.fromString(authentication.getName());
        Page<ViewingScheduleResponse> page = viewingScheduleService
            .getClientSchedules(clientId, status, pageable)
            .map(this::toDto);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách lịch hẹn thành công", page));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — Host actions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Host / Agent xác nhận lịch hẹn đang ở trạng thái PENDING.
     * @PreAuthorize đảm bảo chỉ owner/agent của Listing mới được gọi endpoint này (chống IDOR).
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("@viewingScheduleSecurity.isListingOwner(#id)")
    @Operation(summary = "Xác nhận lịch hẹn",
        description = "Xác nhận lịch hẹn đang ở trạng thái PENDING")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> confirmSchedule(
            @PathVariable UUID id) {
        ViewingSchedule saved = viewingScheduleService.confirmSchedule(id);
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận lịch hẹn xem nhà thành công", toDto(saved)));
    }

    /**
     * Host / Agent huỷ lịch hẹn kèm lý do.
     * @PreAuthorize đảm bảo chỉ owner/agent của Listing mới được gọi endpoint này (chống IDOR).
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@viewingScheduleSecurity.isListingOwner(#id)")
    @Operation(summary = "Huỷ lịch hẹn",
        description = "Host/Agent huỷ lịch hẹn kèm lý do (cancel_reason)")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> cancelSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertViewingScheduleRequest cancelRequest,
            Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        ViewingSchedule saved = viewingScheduleService.cancelSchedule(
            id, cancelRequest.getReason(), actorId);
        return ResponseEntity.ok(ApiResponse.ok("Huỷ lịch hẹn xem nhà thành công", toDto(saved)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Map ViewingSchedule entity → ViewingScheduleResponse DTO.
     * Các trường tên khác nhau (scheduledStart/scheduledEnd) được map thủ công.
     */
    private ViewingScheduleResponse toDto(ViewingSchedule entity) {
        ViewingScheduleResponse dto = modelMapper.map(entity, ViewingScheduleResponse.class);
        dto.setScheduledUtcTime(entity.getScheduledStart());
        dto.setScheduledEndUtcTime(entity.getScheduledEnd());
        return dto;
    }
}