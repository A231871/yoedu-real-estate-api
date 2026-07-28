package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ReportRepository;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationListingSummaryResponse> getPendingListings(Pageable pageable) {
        Page<Listing> listings = listingRepository.findByStatusAndDeletedAtIsNull(ListingStatus.PENDING, pageable);
        return listings.map(this::toModerationListingSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        ReportStatus filterStatus = status != null ? status : ReportStatus.PENDING;
        Page<Report> reports = reportRepository.findByStatusAndDeletedAtIsNull(filterStatus, pageable);
        return reports.map(this::toReportResponse);
    }

    private ModerationListingSummaryResponse toModerationListingSummaryResponse(Listing listing) {
        BigDecimal currentPrice = (listing.getPrices() != null && !listing.getPrices().isEmpty())
            ? listing.getPrices().get(0).getAmountVND()
            : null;

        return new ModerationListingSummaryResponse(
            listing.getId(),
            listing.getTitle(),
            listing.getSlug(),
            listing.getAddress(),
            listing.getArea(),
            currentPrice,
            listing.getListingType() != null ? listing.getListingType().name() : null,
            listing.getPropertyType() != null ? listing.getPropertyType().getName() : null,
            listing.getOwner() != null ? listing.getOwner().getId() : null,
            listing.getOwner() != null ? listing.getOwner().getFullName() : null,
            listing.getStatus() != null ? listing.getStatus().name() : null,
            listing.getCreatedAt()
        );
    }

    private ReportResponse toReportResponse(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getListing() != null ? report.getListing().getId() : null,
            report.getListing() != null ? report.getListing().getTitle() : null,
            report.getReporter() != null ? report.getReporter().getId() : null,
            report.getReporter() != null ? report.getReporter().getFullName() : null,
            report.getReason() != null ? report.getReason().name() : null,
            report.getDescription(),
            report.getStatus() != null ? report.getStatus().name() : null,
            report.getAdminNote(),
            report.getResolvedBy() != null ? report.getResolvedBy().getId() : null,
            report.getResolvedBy() != null ? report.getResolvedBy().getFullName() : null,
            report.getResolvedAt(),
            report.getCreatedAt()
        );
    }
}
