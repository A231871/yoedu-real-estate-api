package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.AuditLog;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.domain.listings.api.ListingAuditApi;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.repository.AuditLogRepository;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ReportRepository;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final ListingAuditApi listingAuditApi;

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

    @Override
    @Transactional(readOnly = true)
    public List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new NotFoundException("Listing not found: " + listingId);
        }
        return listingAuditApi.getListingAuditHistory(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getSystemAuditLogs(UUID actorId, String entityType, String entityId, Pageable pageable) {
        // Flaw 2 fix: use JPA Specification to dynamically combine all non-null filters.
        // Flaw 7 fix: apply a LEFT JOIN FETCH on actor inside the spec to avoid N+1 queries,
        // since JpaSpecificationExecutor.findAll(Specification, Pageable) does not honour
        // the @EntityGraph overrides declared on the derived-query methods.
        Specification<AuditLog> spec = buildAuditLogSpec(actorId, entityType, entityId);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);
        return auditLogs.map(this::toAuditLogResponse);
    }

    /**
     * Builds a JPA Specification for AuditLog combining all non-null filter params.
     * A LEFT JOIN FETCH on "actor" is included to eagerly load the actor and avoid N+1 queries.
     */
    private Specification<AuditLog> buildAuditLogSpec(UUID actorId, String entityType, String entityId) {
        return (root, query, cb) -> {
            // Eagerly fetch actor to prevent N+1 — only applied on the data query (not count query)
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("actor", JoinType.LEFT);
            }

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (actorId != null) {
                predicates.add(cb.equal(root.get("actor").get("id"), actorId));
            }
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }

            return predicates.isEmpty()
                ? cb.conjunction()
                : cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
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

    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getActor() != null ? log.getActor().getId() : null,
            log.getActor() != null ? log.getActor().getFullName() : null,
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getOldValue(),
            log.getNewValue(),
            log.getIpAddress(),
            log.getUserAgent(),
            log.getCreatedAt()
        );
    }
}
