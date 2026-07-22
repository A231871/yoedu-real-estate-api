package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.view_schedule.CancelViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.view_schedule.CreateViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.view_schedule.ViewingScheduleResponse;
import com.yoedu.yoedurealestateapi.service.ViewingScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
 */
@RestController
@RequestMapping("/api/viewing-schedules")
@RequiredArgsConstructor
@Tag(name = "Viewing Schedules", description = "APIs quản lý lịch hẹn xem nhà")
public class ViewingScheduleController {

    private final ViewingScheduleService viewingScheduleService;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────


    @PostMapping
    @Operation(summary = "Đặt lịch hẹn xem nhà", description = "Renter tạo yêu cầu xem nhà cho listing có trạng thái APPROVED")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> createSchedule(
            @Valid @RequestBody CreateViewingScheduleRequest request,
            Authentication authentication) {
        UUID clientId = UUID.fromString(authentication.getName());
        ViewingScheduleResponse saved = viewingScheduleService.createSchedule(request, clientId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo lịch hẹn xem nhà thành công", saved));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────


    @GetMapping("/{id}")
    @PreAuthorize("@viewingScheduleSecurity.isParticipant(#id)")
    @Operation(summary = "Lấy chi tiết lịch hẹn", description = "Xem thông tin một lịch hẹn cụ thể — chỉ client hoặc host/agent mới có quyền")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> getById(
            @PathVariable UUID id) {
        ViewingScheduleResponse schedule = viewingScheduleService.getScheduleById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin lịch hẹn thành công", schedule));
    }


    @GetMapping("/managed")
    @Operation(summary = "Lấy lịch hẹn quản lý", description = "Xem danh sách lịch hẹn của các listing đang được quản lý, lọc theo status tuỳ chọn")
    public ResponseEntity<ApiResponse<Page<ViewingScheduleResponse>>> getManagedSchedules(
            @RequestParam(required = false) List<String> status,
            Pageable pageable,
            Authentication authentication) {
        UUID hostId = UUID.fromString(authentication.getName());
        Page<ViewingScheduleResponse> page = viewingScheduleService
                .getHostSchedules(hostId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch hẹn thành công", page));
    }


    @GetMapping("/requested")
    @Operation(summary = "Lấy lịch hẹn đã yêu cầu", description = "Xem danh sách lịch hẹn đã yêu cầu, lọc theo status tuỳ chọn")
    public ResponseEntity<ApiResponse<Page<ViewingScheduleResponse>>> getRequestedSchedules(
            @RequestParam(required = false) List<String> status,
            Pageable pageable,
            Authentication authentication) {
        UUID clientId = UUID.fromString(authentication.getName());
        Page<ViewingScheduleResponse> page = viewingScheduleService
                .getClientSchedules(clientId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch hẹn thành công", page));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE — Host actions
    // ─────────────────────────────────────────────────────────────────────────


    @PutMapping("/{id}/confirm")
    @PreAuthorize("@viewingScheduleSecurity.isListingOwner(#id)")
    @Operation(summary = "Xác nhận lịch hẹn", description = "Xác nhận lịch hẹn đang ở trạng thái PENDING_CONFIRMATION")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> confirmSchedule(
            @PathVariable UUID id) {
        ViewingScheduleResponse saved = viewingScheduleService.confirmSchedule(id);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận lịch hẹn xem nhà thành công", saved));
    }


    @PutMapping("/{id}/cancel")
    @PreAuthorize("@viewingScheduleSecurity.isParticipant(#id)")
    @Operation(summary = "Huỷ lịch hẹn", description = "Client hoặc Host/Agent huỷ lịch hẹn kèm lý do (cancel_reason)")
    public ResponseEntity<ApiResponse<ViewingScheduleResponse>> cancelSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody CancelViewingScheduleRequest cancelRequest,
            Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        ViewingScheduleResponse saved = viewingScheduleService.cancelSchedule(
                id, cancelRequest.getReason(), actorId);
        return ResponseEntity.ok(ApiResponse.success("Huỷ lịch hẹn xem nhà thành công", saved));
    }
}