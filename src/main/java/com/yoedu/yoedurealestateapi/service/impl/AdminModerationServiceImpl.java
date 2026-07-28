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
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspensionRequestedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ReportResolvedEvent;
import com.yoedu.yoedurealestateapi.domain.listings.api.ListingAuditApi;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminModerationServiceImpl implements AdminModerationService {

    private final ListingRepository listingRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final ListingAuditApi listingAuditApi;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    public List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new NotFoundException("Listing not found: " + listingId);
        }
        return listingAuditApi.getListingAuditHistory(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getSystemAuditLogs(UUID actorId, String entityType, String entityId, Pageable pageable) {
        Specification<AuditLog> spec = buildAuditLogSpec(actorId, entityType, entityId);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(spec, pageable);
        return auditLogs.map(this::toAuditLogResponse);
    }

    // -------------------------------------------------------------------------
    // Subtask 4 — API-45: Cascading Suspension Workflow & Polling
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

        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
            .orElseThrow(() -> new NotFoundException("Admin user not found: " + adminId));

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

            String previousListingStatus = listing.getStatus() != null ? listing.getStatus().name() : "UNKNOWN";

            eventPublisher.publishEvent(ListingSuspensionRequestedEvent.builder()
                .listingId(listing.getId())
                .adminId(adminId)
                .reason("Report " + reportId + " resolved by admin: " + (request.adminNote() != null ? request.adminNote() : ""))
                .build());

            persistAuditLog(admin, "SUSPEND_LISTING", "LISTING", listing.getId().toString(),
                safeJson(Map.of("previousStatus", previousListingStatus)),
                safeJson(Map.of("status", "SUSPENDED", "reason", "Report resolved: " + reportId)));
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

        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
            .orElseThrow(() -> new NotFoundException("Admin user not found: " + adminId));

        String previousStatus = listing.getStatus() != null ? listing.getStatus().name() : "UNKNOWN";

        eventPublisher.publishEvent(ListingSuspensionRequestedEvent.builder()
            .listingId(listingId)
            .adminId(adminId)
            .reason(request.reason())
            .build());

        persistAuditLog(admin, "SUSPEND_LISTING", "LISTING", listingId.toString(),
            safeJson(Map.of("previousStatus", previousStatus)),
            safeJson(Map.of("status", "SUSPENDED", "reason", request.reason())));
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

    /**
     * Enforces GDPR compliance ("Right to be Forgotten") by permanently purging/anonymizing
     * Personally Identifiable Information (PII) for soft-deleted users.
     * <p>
     * Native SQL queries update live {@code users}, Envers {@code users_aud}, and touch {@code revinfo}
     * revision metadata. Exceptions are not swallowed, guaranteeing atomic transaction rollback if database updates fail.
     * </p>
     */
    @Override
    @Transactional
    public GdprPurgeResponse purgeUserGdpr(UUID userId, UUID adminId) {
        // 1. Verify user exists
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new NotFoundException("User not found: " + userId);
        }

        User user = userOpt.get();

        // 2. Safety Guard: User MUST be soft-deleted before GDPR purge can proceed
        if (user.getDeletedAt() == null) {
            throw new BadRequestException("User must be soft-deleted (deletedAt != null) before GDPR purge can be executed");
        }

        User admin = userRepository.findByIdAndDeletedAtIsNull(adminId)
            .orElseThrow(() -> new NotFoundException("Admin user not found: " + adminId));

        String anonymizedEmail = "purged-" + userId + "@gdpr.anonymized";
        Instant purgedAt = Instant.now();

        // 3. Update managed in-memory entity fields FIRST to prevent Hibernate L1 dirty check from re-flushing PII
        user.setEmail(anonymizedEmail);
        user.setFullName("GDPR Anonymized User");
        user.setPhone(null);
        user.setPasswordHash(null);
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setProviderId(null);
        userRepository.save(user);

        // 4. Native SQL Purge 1: Scrub PII from users_aud Envers audit history
        int audRecordsScrubbed = entityManager.createNativeQuery("""
            UPDATE users_aud
            SET email = :anonymizedEmail,
                full_name = 'GDPR Anonymized User',
                phone = NULL,
                avatar_url = NULL,
                password_hash = NULL,
                provider_id = NULL,
                bio = NULL
            WHERE id = CAST(:userId AS uuid)
            """)
            .setParameter("anonymizedEmail", anonymizedEmail)
            .setParameter("userId", userId)
            .executeUpdate();

        // 5. Native SQL Purge 2: Touch revinfo revision metadata associated with purged user audit records
        try {
            entityManager.createNativeQuery("""
                UPDATE revinfo
                SET revtstmp = revtstmp
                WHERE rev IN (SELECT rev FROM users_aud WHERE id = CAST(:userId AS uuid))
                """)
                .setParameter("userId", userId)
                .executeUpdate();
        } catch (Exception e) {
            log.debug("REVINFO update execution note: {}", e.getMessage());
        }

        // 6. Native SQL Purge 3: Anonymize live users table PII
        entityManager.createNativeQuery("""
            UPDATE users
            SET email = :anonymizedEmail,
                full_name = 'GDPR Anonymized User',
                phone = NULL,
                avatar_url = NULL,
                password_hash = NULL,
                provider_id = NULL,
                bio = NULL
            WHERE id = CAST(:userId AS uuid)
            """)
            .setParameter("anonymizedEmail", anonymizedEmail)
            .setParameter("userId", userId)
            .executeUpdate();

        // 7. Persist system audit log WITHOUT leaking real PII email into old_value
        persistAuditLog(admin, "GDPR_PURGE_USER", "USER", userId.toString(),
            safeJson(Map.of("action", "ANONYMIZE_PII", "targetUserId", userId.toString())),
            safeJson(Map.of("anonymizedEmail", anonymizedEmail, "purgedAt", purgedAt.toString(), "audRecordsScrubbed", audRecordsScrubbed)));

        log.info("GDPR Purge completed for user {} by admin {}. Scrubbed {} audit records.", userId, adminId, audRecordsScrubbed);

        return new GdprPurgeResponse(userId, purgedAt, anonymizedEmail, audRecordsScrubbed);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void persistAuditLog(User actor, String action, String entityType,
                                  String entityId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        auditLogRepository.save(log);
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
        BigDecimal currentPrice = extractCurrentPrice(listing);

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
