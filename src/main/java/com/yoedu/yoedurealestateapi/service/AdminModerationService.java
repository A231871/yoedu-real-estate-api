package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.GdprPurgeResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingStatusResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ResolveReportRequest;
import com.yoedu.yoedurealestateapi.dto.moderation.SuspendListingRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminModerationService {
    Page<ModerationListingSummaryResponse> getPendingListings(Pageable pageable);
    Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable);
    List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId);
    Page<AuditLogResponse> getSystemAuditLogs(UUID actorId, String entityType, String entityId, Pageable pageable);

    // Subtask 4 — API-45: Cascading Suspension Workflow & Polling
    ReportResponse resolveReport(UUID reportId, UUID adminId, ResolveReportRequest request);
    void suspendListing(UUID listingId, UUID adminId, SuspendListingRequest request);
    ListingStatusResponse getListingStatus(UUID listingId);

    // Subtask 5 — API-46: GDPR Native SQL Purge Task
    GdprPurgeResponse purgeUserGdpr(UUID userId, UUID adminId);
}
