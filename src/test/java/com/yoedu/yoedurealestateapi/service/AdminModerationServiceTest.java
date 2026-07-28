package com.yoedu.yoedurealestateapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ListingPrice;
import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.domain.enums.ReportReason;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ReportRepository;
import com.yoedu.yoedurealestateapi.service.impl.AdminModerationServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminModerationServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminModerationServiceImpl adminModerationService;

    private Listing listing;
    private Report report;
    private User owner;
    private User reporter;
    private User adminUser;

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
        report.setStatus(ReportStatus.RESOLVED);
        report.setAdminNote("Đã xử lý gỡ tin");
        report.setResolvedBy(adminUser);
        report.setResolvedAt(Instant.now());
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

        when(reportRepository.findByStatusAndDeletedAtIsNull(eq(ReportStatus.RESOLVED), any(Pageable.class)))
            .thenReturn(reportPage);

        Page<ReportResponse> result = adminModerationService.getReportsByStatus(ReportStatus.RESOLVED, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        ReportResponse dto = result.getContent().get(0);
        assertEquals("FRAUD", dto.reason());
        assertEquals("Tin đăng giả mạo", dto.description());
        assertEquals("Tran Van B", dto.reporterName());
        assertEquals("Admin User", dto.resolvedByName());
        assertNotNull(dto.resolvedAt());
    }
}
