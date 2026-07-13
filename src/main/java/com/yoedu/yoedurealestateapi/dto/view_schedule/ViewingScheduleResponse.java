package com.yoedu.yoedurealestateapi.dto;


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
    private Instant completedAt;
    private boolean reminderSent;
    private Long version;
}