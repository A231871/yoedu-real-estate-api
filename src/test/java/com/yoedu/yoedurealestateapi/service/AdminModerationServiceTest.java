package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.AuditLog;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ListingPrice;
import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.domain.enums.ReportReason;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspensionRequestedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ReportResolvedEvent;
import com.yoedu.yoedurealestateapi.domain.listings.api.ListingAuditApi;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
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
import com.yoedu.yoedurealestateapi.service.impl.AdminModerationServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ListingAuditApi listingAuditApi;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminModerationServiceImpl adminModerationService;

    private Listing listing;
    private Report report;
    private User owner;
    private User reporter;
    private User adminUser;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFullName("Nguyen Van A");

        reporter = new User();
        reporter.setId(UUID.randomUUID());
        reporter.setFullName("Tran Van B");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setFullName("Admin User");

        PropertyType propertyType = new PropertyType();
        propertyType.setId(1);
        propertyType.setName("Căn hộ");

        ListingPrice price = new ListingPrice();
        price.setAmountVND(BigDecimal.valueOf(5000000));

        listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Căn hộ trung tâm");
        listing.setSlug("can-ho-trung-tam");
        listing.setAddress("123 Le Loi");
        listing.setArea(BigDecimal.valueOf(50.5));
        listing.setStatus(ListingStatus.PENDING);
        listing.setListingType(ListingType.FOR_RENT);
        listing.setPropertyType(propertyType);
        listing.setOwner(owner);
        listing.setPrices(List.of(price));

        report = new Report();
        report.setId(UUID.randomUUID());
        report.setListing(listing);
        report.setReporter(reporter);
        report.setReason(ReportReason.FRAUD);
        report.setDescription("Tin đăng giả mạo");
        report.setStatus(ReportStatus.PENDING);
        report.setAdminNote(null);
        report.setResolvedBy(null);
        report.setResolvedAt(null);

        auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setActor(adminUser);
        auditLog.setAction("SUSPEND_LISTING");
        auditLog.setEntityType("LISTING");
        auditLog.setEntityId(listing.getId().toString());
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setCreatedAt(Instant.now());
    }

    @Test
    void getPendingListings_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> listingPage = new PageImpl<>(List.of(listing));

        when(listingRepository.findByStatusAndDeletedAtIsNull(eq(ListingStatus.PENDING), any(Pageable.class)))
            .thenReturn(listingPage);

        Page<ModerationListingSummaryResponse> result = adminModerationService.getPendingListings(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        ModerationListingSummaryResponse dto = result.getContent().get(0);
        assertEquals("Căn hộ trung tâm", dto.title());
        assertEquals(BigDecimal.valueOf(5000000), dto.currentPrice());
        assertEquals("Nguyen Van A", dto.ownerName());
        assertEquals("Căn hộ", dto.propertyTypeName());
    }

    @Test
    void getReportsByStatus_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Report> reportPage = new PageImpl<>(List.of(report));

        when(reportRepository.findByStatusAndDeletedAtIsNull(eq(ReportStatus.PENDING), any(Pageable.class)))
            .thenReturn(reportPage);

        Page<ReportResponse> result = adminModerationService.getReportsByStatus(ReportStatus.PENDING, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        ReportResponse dto = result.getContent().get(0);
        assertEquals("FRAUD", dto.reason());
        assertEquals("Tin đăng giả mạo", dto.description());
        assertEquals("Tran Van B", dto.reporterName());
    }

    @Test
    void getListingAuditHistory_Success() {
        UUID listingId = listing.getId();
        ListingAuditHistoryResponse historyDto = new ListingAuditHistoryResponse(
            1, Instant.now(), "ADD", listingId, "Căn hộ trung tâm", "PENDING"
        );

        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(listingAuditApi.getListingAuditHistory(listingId)).thenReturn(List.of(historyDto));

        List<ListingAuditHistoryResponse> result = adminModerationService.getListingAuditHistory(listingId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Căn hộ trung tâm", result.get(0).title());
    }

    @Test
    void getListingAuditHistory_NotFound_ThrowsResourceNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(listingRepository.existsById(missingId)).thenReturn(false);

        assertThrows(NotFoundException.class,
            () -> adminModerationService.getListingAuditHistory(missingId));
    }

    @Test
    void getSystemAuditLogs_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> logPage = new PageImpl<>(List.of(auditLog));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(logPage);

        Page<AuditLogResponse> result = adminModerationService.getSystemAuditLogs(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SUSPEND_LISTING", result.getContent().get(0).action());
        assertEquals("Admin User", result.getContent().get(0).actorName());
    }

    @Test
    void resolveReport_ResolveAndCascadeSuspend_Success() {
        UUID reportId = report.getId();
        UUID adminId = adminUser.getId();
        ResolveReportRequest request = new ResolveReportRequest("RESOLVED", "Vi phạm chính sách", true);

        when(reportRepository.findWithListingByIdAndDeletedAtIsNull(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(adminUser));

        ReportResponse response = adminModerationService.resolveReport(reportId, adminId, request);

        assertNotNull(response);
        assertEquals("RESOLVED", response.status());

        verify(reportRepository).save(report);
        verify(eventPublisher).publishEvent(any(ListingSuspensionRequestedEvent.class));
        verify(eventPublisher).publishEvent(any(ReportResolvedEvent.class));
    }

    @Test
    void resolveReport_AlreadyResolved_ThrowsBadRequest() {
        UUID reportId = report.getId();
        UUID adminId = adminUser.getId();
        report.setStatus(ReportStatus.RESOLVED);
        ResolveReportRequest request = new ResolveReportRequest("RESOLVED", "Note", false);

        when(reportRepository.findWithListingByIdAndDeletedAtIsNull(reportId)).thenReturn(Optional.of(report));

        assertThrows(BadRequestException.class,
            () -> adminModerationService.resolveReport(reportId, adminId, request));
    }

    @Test
    void suspendListing_Success() {
        UUID listingId = listing.getId();
        UUID adminId = adminUser.getId();
        SuspendListingRequest request = new SuspendListingRequest("Thông tin sai sự thật");

        when(listingRepository.findWithOwnerByIdAndDeletedAtIsNull(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findByIdAndDeletedAtIsNull(adminId)).thenReturn(Optional.of(adminUser));

        adminModerationService.suspendListing(listingId, adminId, request);

        verify(eventPublisher).publishEvent(any(ListingSuspensionRequestedEvent.class));
    }

    @Test
    void suspendListing_AlreadySuspended_ThrowsBadRequest() {
        UUID listingId = listing.getId();
        UUID adminId = adminUser.getId();
        listing.setStatus(ListingStatus.SUSPENDED);
        SuspendListingRequest request = new SuspendListingRequest("Reason");

        when(listingRepository.findWithOwnerByIdAndDeletedAtIsNull(listingId)).thenReturn(Optional.of(listing));

        assertThrows(BadRequestException.class,
            () -> adminModerationService.suspendListing(listingId, adminId, request));
    }

    @Test
    void getListingStatus_Success() {
        UUID listingId = listing.getId();
        when(listingRepository.findStatusById(listingId)).thenReturn(Optional.of(ListingStatus.PENDING));

        ListingStatusResponse response = adminModerationService.getListingStatus(listingId);

        assertNotNull(response);
        assertEquals(listingId, response.listingId());
        assertEquals("PENDING", response.status());
    }
}
