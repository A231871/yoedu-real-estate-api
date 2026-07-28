package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingStatusResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ResolveReportRequest;
import com.yoedu.yoedurealestateapi.dto.moderation.SuspendListingRequest;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Moderation", description = "Admin moderation queues, polling, suspension, and audit endpoints")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    @GetMapping("/listings/pending")
    @Operation(summary = "Get pending listings", description = "Retrieves paginated listings with PENDING status for admin moderation")
    public ResponseEntity<ApiResponse<Page<ModerationListingSummaryResponse>>> getPendingListings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ModerationListingSummaryResponse> result = adminModerationService.getPendingListings(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bất động sản chờ duyệt thành công", result));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get user reports", description = "Retrieves paginated user reports filtered by status (default PENDING)")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @RequestParam(required = false, defaultValue = "PENDING") ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ReportResponse> result = adminModerationService.getReportsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách báo cáo vi phạm thành công", result));
    }

    @GetMapping("/listings/{id}/audit-history")
    @Operation(summary = "Get listing audit history", description = "Retrieves Envers revision history for a specific listing")
    public ResponseEntity<ApiResponse<List<ListingAuditHistoryResponse>>> getListingAuditHistory(@PathVariable UUID id) {
        List<ListingAuditHistoryResponse> result = adminModerationService.getListingAuditHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử chỉnh sửa bất động sản thành công", result));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get system audit logs", description = "Retrieves paginated system audit logs with optional actorId and entity filters")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getSystemAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLogResponse> result = adminModerationService.getSystemAuditLogs(actorId, entityType, entityId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký hoạt động hệ thống thành công", result));
    }

    @GetMapping("/listings/{id}/status")
    @Operation(summary = "Get listing status for polling", description = "Lightweight status endpoint for frontend polling following async suspension requests")
    public ResponseEntity<ApiResponse<ListingStatusResponse>> getListingStatus(@PathVariable UUID id) {
        ListingStatusResponse result = adminModerationService.getListingStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái bất động sản thành công", result));
    }

    @PutMapping("/reports/{id}/resolve")
    @Operation(
        summary = "Resolve a user report",
        description = "Marks a report as RESOLVED or DISMISSED. If resolution=RESOLVED and suspendListing=true, " +
                      "emits a ListingSuspensionRequestedEvent for async suspension and notifies the listing owner."
    )
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
            @PathVariable UUID id,
            @Valid @RequestBody ResolveReportRequest request,
            Authentication authentication) {
        UUID adminId = parseAdminId(authentication);
        ReportResponse result = adminModerationService.resolveReport(id, adminId, request);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo vi phạm đã được xử lý thành công", result));
    }

    @PostMapping("/listings/{id}/suspend")
    @Operation(
        summary = "Suspend a listing (Async Command)",
        description = "Emits an asynchronous ListingSuspensionRequestedEvent, returns 202 ACCEPTED immediately, " +
                      "and notifies the listing owner after commit."
    )
    public ResponseEntity<ApiResponse<Void>> suspendListingPost(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendListingRequest request,
            Authentication authentication) {
        UUID adminId = parseAdminId(authentication);
        adminModerationService.suspendListing(id, adminId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Yêu cầu tạm dừng bất động sản đã được ghi nhận", null));
    }

    @PutMapping("/listings/{id}/suspend")
    @Operation(summary = "Suspend a listing (PUT alias)", description = "Alias for POST /listings/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendListingPut(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendListingRequest request,
            Authentication authentication) {
        return suspendListingPost(id, request, authentication);
    }

    /**
     * Safely extracts and validates the admin UUID from the Spring Security Authentication context.
     */
    private UUID parseAdminId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BadRequestException("Unauthenticated admin request context");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid admin principal UUID: " + authentication.getName());
        }
    }
}
