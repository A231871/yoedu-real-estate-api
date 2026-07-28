package com.yoedu.yoedurealestateapi.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yoedu.yoedurealestateapi.common.exception.GlobalExceptionHandler;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.AuditLogResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.GdprPurgeResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingStatusResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ResolveReportRequest;
import com.yoedu.yoedurealestateapi.dto.moderation.SuspendListingRequest;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.service.AdminModerationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminModerationControllerTest {

    private MockMvc mockMvc;
    private AdminModerationService adminModerationService;
    private UserRepository userRepository;
    private UUID mockAdminId;

    @BeforeEach
    void setUp() {
        mockAdminId = UUID.randomUUID();
        adminModerationService = mock(AdminModerationService.class);
        userRepository = mock(UserRepository.class);
        AdminModerationController controller = new AdminModerationController(adminModerationService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getPendingListings_Returns200OK() throws Exception {
        ModerationListingSummaryResponse dto = new ModerationListingSummaryResponse(
            UUID.randomUUID(), "Nhà mặt tiền", "nha-mat-tien", "123 Pham Van Dong",
            BigDecimal.valueOf(100), BigDecimal.valueOf(10000000), "FOR_RENT", "Nhà nguyên căn",
            UUID.randomUUID(), "Nguyen Van Owner", "PENDING", Instant.now()
        );
        Page<ModerationListingSummaryResponse> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(adminModerationService.getPendingListings(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/moderation/listings/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].title", is("Nhà mặt tiền")))
                .andExpect(jsonPath("$.data.content[0].ownerName", is("Nguyen Van Owner")));
    }

    @Test
    void getReports_Returns200OK() throws Exception {
        UUID adminId = UUID.randomUUID();
        ReportResponse dto = new ReportResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Nhà mặt tiền",
            UUID.randomUUID(), "Tran Reporter", "FRAUD", "Tin gia",
            "RESOLVED", "Da xu ly", adminId, "Admin User", Instant.now(), Instant.now()
        );
        Page<ReportResponse> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(adminModerationService.getReportsByStatus(eq(ReportStatus.RESOLVED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/admin/moderation/reports").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].reason", is("FRAUD")))
                .andExpect(jsonPath("$.data.content[0].resolvedByName", is("Admin User")));
    }

    @Test
    void getListingAuditHistory_Returns200OK() throws Exception {
        UUID listingId = UUID.randomUUID();
        ListingAuditHistoryResponse dto = new ListingAuditHistoryResponse(
            1, Instant.now(), "ADD", listingId, "Nhà mặt tiền", "PENDING"
        );

        when(adminModerationService.getListingAuditHistory(listingId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/moderation/listings/{id}/audit-history", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[0].title", is("Nhà mặt tiền")))
                .andExpect(jsonPath("$.data[0].revisionType", is("ADD")));
    }

    @Test
    void getSystemAuditLogs_Returns200OK() throws Exception {
        AuditLogResponse dto = new AuditLogResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Admin User", "SUSPEND_LISTING",
            "LISTING", UUID.randomUUID().toString(), null, null, "127.0.0.1", "Postman", Instant.now()
        );
        Page<AuditLogResponse> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(adminModerationService.getSystemAuditLogs(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/moderation/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].action", is("SUSPEND_LISTING")))
                .andExpect(jsonPath("$.data.content[0].actorName", is("Admin User")));
    }

    @Test
    void getListingStatus_Returns200OK() throws Exception {
        UUID listingId = UUID.randomUUID();
        ListingStatusResponse responseDto = new ListingStatusResponse(listingId, "SUSPENDED");

        when(adminModerationService.getListingStatus(listingId)).thenReturn(responseDto);

        mockMvc.perform(get("/admin/moderation/listings/{id}/status", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")));
    }

    @Test
    void resolveReport_Returns200OK() throws Exception {
        UUID reportId = UUID.randomUUID();
        ReportResponse responseDto = new ReportResponse(
            reportId, UUID.randomUUID(), "Listing Title",
            UUID.randomUUID(), "Reporter Name", "FRAUD", "Fake listing",
            "RESOLVED", "Handled", mockAdminId, "Admin User", Instant.now(), Instant.now()
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(mockAdminId.toString(), null);

        when(adminModerationService.resolveReport(eq(reportId), eq(mockAdminId), any(ResolveReportRequest.class)))
            .thenReturn(responseDto);

        mockMvc.perform(put("/admin/moderation/reports/{id}/resolve", reportId)
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "resolution": "RESOLVED",
                      "adminNote": "Handled",
                      "suspendListing": true
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));
    }

    @Test
    void suspendListing_Returns202Accepted() throws Exception {
        UUID listingId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(mockAdminId.toString(), null);

        mockMvc.perform(post("/admin/moderation/listings/{id}/suspend", listingId)
                .principal(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason": "Violated terms of service"
                    }
                    """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success", is(true)));

        verify(adminModerationService).suspendListing(eq(listingId), eq(mockAdminId), any(SuspendListingRequest.class));
    }

    @Test
    void purgeUserGdpr_Returns200OK() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(mockAdminId.toString(), null);
        GdprPurgeResponse responseDto = new GdprPurgeResponse(targetUserId, Instant.now(), "purged-" + targetUserId + "@gdpr.anonymized", 2);

        when(adminModerationService.purgeUserGdpr(eq(targetUserId), eq(mockAdminId))).thenReturn(responseDto);

        mockMvc.perform(delete("/admin/moderation/users/{userId}/gdpr-purge", targetUserId)
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.anonymizedEmail", is("purged-" + targetUserId + "@gdpr.anonymized")));
    }
}
