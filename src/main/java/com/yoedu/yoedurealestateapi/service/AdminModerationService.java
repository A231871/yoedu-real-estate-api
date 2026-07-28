package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminModerationService {
    Page<ModerationListingSummaryResponse> getPendingListings(Pageable pageable);
    Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable);
    List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId);
    Page<AuditLogResponse> getSystemAuditLogs(UUID actorId, String entityType, String entityId, Pageable pageable);
}
