package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "viewing_schedules")
public class ViewingSchedule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;


    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    // Ánh xạ scheduled_utc_time tới cột scheduled_start (TIMESTAMPTZ) [2]
    @Column(name = "scheduled_start", nullable = false)
    private Instant scheduledStart;

    // Ánh xạ scheduled_end_utc_time tới cột scheduled_end (TIMESTAMPTZ) [2]
    @Column(name = "scheduled_end", nullable = false)
    private Instant scheduledEnd;

    // Thuộc tính ảo tự động sinh trong cơ sở dữ liệu (GENERATED ALWAYS) [2]
    @Column(name = "duration_mins", insertable = false, updatable = false)
    private Integer durationMins;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "reminder_sent", nullable = false)
    private boolean reminderSent;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Múi giờ địa phương phục vụ tính toán DST chuẩn xác (được thêm qua V11) [2]
    @Column(name = "scheduled_local_time", nullable = false)
    private LocalDateTime scheduledLocalTime;

    @Column(name = "timezone_id", nullable = false, length = 100)
    private String timezoneId;

    // Khóa lạc quan chống xung đột sửa đổi đồng thời (được thêm qua V11) [2]
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}