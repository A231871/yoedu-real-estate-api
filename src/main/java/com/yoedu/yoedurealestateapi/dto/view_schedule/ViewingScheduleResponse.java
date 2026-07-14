package com.yoedu.yoedurealestateapi.dto.view_schedule;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
@Setter
@Getter
public class ViewingScheduleResponse {
    private UUID id;
    private UUID listingId;
    private UUID clientId;
    private UUID hostId;
    private LocalDateTime scheduledLocalTime;
    private Instant scheduledUtcTime;
    private Instant scheduledEndUtcTime;
    private Integer durationMins;
    private String timezoneId;
    private String status;
    private String note;
    private String cancelReason;
    private UUID cancelledBy;
    private Instant cancelledAt;
    private Instant confirmedAt;
    private Instant confirmationPromptedAt;
    private Instant completedAt;
    private boolean reminderSent;
    private Long version;
}