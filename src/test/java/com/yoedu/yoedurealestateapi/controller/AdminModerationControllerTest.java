package com.yoedu.yoedurealestateapi.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yoedu.yoedurealestateapi.common.exception.GlobalExceptionHandler;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import com.yoedu.yoedurealestateapi.dto.moderation.ModerationListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.moderation.ReportResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminModerationControllerTest {

    private MockMvc mockMvc;
    private AdminModerationService adminModerationService;

    @BeforeEach
    void setUp() {
        adminModerationService = mock(AdminModerationService.class);
        AdminModerationController controller = new AdminModerationController(adminModerationService);
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
}
