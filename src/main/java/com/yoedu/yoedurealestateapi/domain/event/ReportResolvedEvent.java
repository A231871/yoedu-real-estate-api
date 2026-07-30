package com.yoedu.yoedurealestateapi.domain.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResolvedEvent {
    private UUID reportId;
    private UUID listingId;
    private String listingTitle;
    private UUID ownerId;
    private UUID adminId;
    private String resolution;  // "RESOLVED" or "DISMISSED"
    private String adminNote;
}
