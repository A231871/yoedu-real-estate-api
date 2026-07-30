package com.yoedu.yoedurealestateapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.AuditLog;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ListingPrice;
import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspendedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ReportResolvedEvent;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.GdprPurgeResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingStatusResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ResolveReportRequest;
import com.yoedu.yoedurealestateapi.dto.moderation.SuspendListingRequest;
import com.yoedu.yoedurealestateapi.repository.AuditLogRepository;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ReportRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public AdminModerationServiceImpl(
            ListingRepository listingRepository,
            ReportRepository reportRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            EntityManager entityManager,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // -------------------------------------------------------------------------
    // Subtask 2 — Read endpoints
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Subtask 3 — Audit endpoints
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new NotFoundException("Listing not found: " + listingId);
        }

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<Object[]> results = auditReader.createQuery()
            .forRevisionsOfEntity(Listing.class, false, true)
            .add(AuditEntity.id().eq(listingId))
            .addOrder(AuditEntity.revisionNumber().asc())
            .getResultList();

        List<ListingAuditHistoryResponse> history = new ArrayList<>();
        for (Object[] row : results) {
            Listing listingSnapshot = (Listing) row[0];
            DefaultRevisionEntity revEntity = (DefaultRevisionEntity) row[1];
            RevisionType revType = (RevisionType) row[2];

            Instant revisedAt = Instant.ofEpochMilli(revEntity.getTimestamp());

            UUID snapshotId = listingSnapshot != null ? listingSnapshot.getId() : listingId;
            String title = listingSnapshot != null ? listingSnapshot.getTitle() : null;
            String status = (listingSnapshot != null && listingSnapshot.getStatus() != null)
                ? listingSnapshot.getStatus().name()
                : null;

            history.add(new ListingAuditHistoryResponse(
                revEntity.getId(),
                revisedAt,
                revType.name(),
                snapshotId,
                title,
                status
            ));
        }

        return history;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getSystemAuditLogs(UUID actorId, String entityType, String entityId, Pageable pageable) {
        Specification<AuditLog> spec = buildAuditLogSpec(actorId, entityType, entityId);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);
        return auditLogs.map(this::toAuditLogResponse);
    }

    // -------------------------------------------------------------------------
    // Subtask 4 — API-45: Suspension Workflow & Polling
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ReportResponse resolveReport(UUID reportId, UUID adminId, ResolveReportRequest request) {
        String resolution = request.resolution().toUpperCase();
        if (!resolution.equals("RESOLVED") && !resolution.equals("DISMISSED")) {
            throw new BadRequestException("resolution must be RESOLVED or DISMISSED");
        }

        Report report = reportRepository.findWithListingByIdAndDeletedAtIsNull(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));

        if (report.getStatus() == ReportStatus.RESOLVED || report.getStatus() == ReportStatus.DISMISSED) {
            throw new BadRequestException("Report is already in a terminal state: " + report.getStatus());
        }

        User admin = requireAdmin(adminId);

        String previousReportStatus = report.getStatus() != null ? report.getStatus().name() : "UNKNOWN";

        report.setStatus(ReportStatus.valueOf(resolution));
        report.setResolvedBy(admin);
        report.setResolvedAt(Instant.now());
        report.setAdminNote(request.adminNote());
        reportRepository.save(report);

        Listing listing = report.getListing();
        UUID listingId = listing != null ? listing.getId() : null;

        if ("RESOLVED".equals(resolution) && Boolean.TRUE.equals(request.suspendListing())) {
            if (listing == null) {
                throw new BadRequestException("Report " + reportId + " is not linked to an active listing");
            }
            applySuspension(listing, adminId, "Report " + reportId + " resolved by admin: "
                + (request.adminNote() != null ? request.adminNote() : ""), admin);
        }

        persistAuditLog(admin, "RESOLVE_REPORT", "REPORT", reportId.toString(),
            safeJson(Map.of("previousStatus", previousReportStatus)),
            safeJson(Map.of("status", resolution, "adminNote", request.adminNote() != null ? request.adminNote() : "")));

        eventPublisher.publishEvent(ReportResolvedEvent.builder()
            .reportId(reportId)
            .listingId(listingId)
            .listingTitle(listing != null ? listing.getTitle() : null)
            .ownerId(listing != null && listing.getOwner() != null ? listing.getOwner().getId() : null)
            .adminId(adminId)
            .resolution(resolution)
            .adminNote(request.adminNote())
            .build());

        return toReportResponse(report);
    }

    @Override
    @Transactional
    public void suspendListing(UUID listingId, UUID adminId, SuspendListingRequest request) {
        Listing listing = listingRepository.findWithOwnerByIdAndDeletedAtIsNull(listingId)
            .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));

        if (listing.getStatus() == ListingStatus.SUSPENDED) {
            throw new BadRequestException("Listing is already SUSPENDED");
        }

        User admin = requireAdmin(adminId);

        applySuspension(listing, adminId, request.reason(), admin);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingStatusResponse getListingStatus(UUID listingId) {
        ListingStatus status = listingRepository.findStatusById(listingId)
            .orElseThrow(() -> new NotFoundException("Listing not found: " + listingId));
        return new ListingStatusResponse(listingId, status.name());
    }

    // -------------------------------------------------------------------------
    // Subtask 5 — API-46: GDPR Native SQL Purge Task
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public GdprPurgeResponse purgeUserGdpr(UUID targetUserId, UUID adminId) {
        Optional<User> userOpt = userRepository.findById(targetUserId);
        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found: " + targetUserId);
        }

        User user = userOpt.get();

        if (user.getDeletedAt() == null) {
            throw new BadRequestException("User must be soft-deleted (deletedAt != null) before GDPR purge can be executed");
        }

        User admin = requireAdmin(adminId);

        String anonymizedEmail = "purged-" + targetUserId + "@gdpr.anonymized";
        Instant purgedAt = Instant.now();

        // Update managed entity in memory first to prevent Hibernate L1 cache from re-flushing old PII
        user.setEmail(anonymizedEmail);
        user.setFullName("GDPR Anonymized User");
        user.setPhone(null);
        user.setPasswordHash(null);
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setProviderId(null);
        userRepository.save(user);

        // Native SQL Purge 1: Scrub users_aud (Envers audit history)
        int audRowsScrubbed = 0;
        try {
            audRowsScrubbed = entityManager.createNativeQuery("""
                UPDATE users_aud
                SET email = :anonymizedEmail,
                    full_name = 'GDPR Anonymized User',
                    phone = NULL,
                    avatar_url = NULL,
                    bio = NULL,
                    password_hash = NULL
                WHERE id = CAST(:userId AS uuid)
                """)
                .setParameter("anonymizedEmail", anonymizedEmail)
                .setParameter("userId", targetUserId.toString())
                .executeUpdate();

            log.info("Scrubbed {} audit rows in users_aud for user {}", audRowsScrubbed, targetUserId);
        } catch (Exception e) {
            log.error("Failed to scrub users_aud for user {}", targetUserId, e);
            throw new RuntimeException("GDPR purge failed during users_aud scrubbing", e);
        }

        persistAuditLog(admin, "GDPR_PURGE_USER", "USER", targetUserId.toString(),
            safeJson(Map.of("action", "ANONYMIZE_PII", "targetUserId", targetUserId.toString())),
            safeJson(Map.of("anonymizedEmail", anonymizedEmail, "purgedAt", purgedAt.toString(), "audRowsScrubbed", audRowsScrubbed)));

        log.info("GDPR Purge completed for user {} by admin {}. Scrubbed {} audit records.", targetUserId, adminId, audRowsScrubbed);

        return new GdprPurgeResponse(targetUserId, purgedAt, anonymizedEmail, audRowsScrubbed);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User requireAdmin(UUID adminId) {
        return userRepository.findByIdAndDeletedAtIsNull(adminId)
            .orElseThrow(() -> new NotFoundException("Admin user not found: " + adminId));
    }

    /**
     * Directly sets the listing status to SUSPENDED and publishes {@link ListingSuspendedEvent}
     * within the current transaction. Replaces the former Modulith two-event chain
     * (ListingSuspensionRequestedEvent -> ListingSuspensionListener -> ListingSuspendedEvent).
     */
    private void applySuspension(Listing listing, UUID adminId, String reason, User admin) {
        String previousStatus = listing.getStatus() != null ? listing.getStatus().name() : "UNKNOWN";

        listing.setStatus(ListingStatus.SUSPENDED);
        listingRepository.save(listing);

        persistAuditLog(admin, "SUSPEND_LISTING", "LISTING", listing.getId().toString(),
            safeJson(Map.of("previousStatus", previousStatus)),
            safeJson(Map.of("status", "SUSPENDED", "reason", reason)));

        eventPublisher.publishEvent(ListingSuspendedEvent.builder()
            .listingId(listing.getId())
            .listingTitle(listing.getTitle())
            .ownerId(listing.getOwner() != null ? listing.getOwner().getId() : null)
            .adminId(adminId)
            .reason(reason)
            .build());
    }

    private void persistAuditLog(User actor, String action, String entityType,
                                  String entityId, String oldValue, String newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLogRepository.save(auditLog);
    }

    private String safeJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Failed to serialize audit log JSON payload", e);
            return "{}";
        }
    }

    private Specification<AuditLog> buildAuditLogSpec(UUID actorId, String entityType, String entityId) {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
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
        return new ModerationListingSummaryResponse(
            listing.getId(),
            listing.getTitle(),
            listing.getSlug(),
            listing.getAddress(),
            listing.getArea(),
            extractCurrentPrice(listing),
            listing.getListingType() != null ? listing.getListingType().name() : null,
            listing.getPropertyType() != null ? listing.getPropertyType().getName() : null,
            listing.getOwner() != null ? listing.getOwner().getId() : null,
            listing.getOwner() != null ? listing.getOwner().getFullName() : null,
            listing.getStatus() != null ? listing.getStatus().name() : null,
            listing.getCreatedAt()
        );
    }

    private BigDecimal extractCurrentPrice(Listing listing) {
        if (listing == null || listing.getPrices() == null || listing.getPrices().isEmpty()) {
            return null;
        }
        ListingPrice firstPrice = listing.getPrices().get(0);
        return firstPrice != null ? firstPrice.getAmountVND() : null;
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

    private AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return new AuditLogResponse(
            auditLog.getId(),
            auditLog.getActor() != null ? auditLog.getActor().getId() : null,
            auditLog.getActor() != null ? auditLog.getActor().getFullName() : null,
            auditLog.getAction(),
            auditLog.getEntityType(),
            auditLog.getEntityId(),
            auditLog.getOldValue(),
            auditLog.getNewValue(),
            auditLog.getIpAddress(),
            auditLog.getUserAgent(),
            auditLog.getCreatedAt()
        );
    }
}
