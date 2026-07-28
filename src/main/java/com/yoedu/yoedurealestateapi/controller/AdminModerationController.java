package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Moderation", description = "Admin moderation queues, polling, and audit endpoints")
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
}
