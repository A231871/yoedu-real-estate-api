package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminModerationService {
    Page<ModerationListingSummaryResponse> getPendingListings(Pageable pageable);
    Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable);
}
