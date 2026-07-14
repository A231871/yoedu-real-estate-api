package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.domain.enums.ViewingScheduleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ViewingScheduleRepository extends JpaRepository<ViewingSchedule, UUID> {

    // GET by ID — chỉ lấy record chưa bị soft-delete
    Optional<ViewingSchedule> findByIdAndDeletedAtIsNull(UUID id);

    // GET /managed — không lọc status (lấy tất cả)
    Page<ViewingSchedule> findByHostIdAndDeletedAtIsNullOrderByScheduledStartDesc(
            UUID hostId, Pageable pageable);

    // GET /managed?status=PENDING,CONFIRMED — lọc theo status
    Page<ViewingSchedule> findByHostIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
            UUID hostId, List<ViewingScheduleStatus> status, Pageable pageable);

    // GET /requested — không lọc status
    Page<ViewingSchedule> findByClientIdAndDeletedAtIsNullOrderByScheduledStartDesc(
            UUID clientId, Pageable pageable);

    // GET /requested?status=CONFIRMED — lọc theo status
    Page<ViewingSchedule> findByClientIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
            UUID clientId, List<ViewingScheduleStatus> status, Pageable pageable);

    /**
     * Transition past CONFIRMED schedules to REQUIRES_FEEDBACK and stamp confirmation_prompted_at.
     */
    @Modifying
    @Query(value = """
            UPDATE viewing_schedules
            SET status = 'REQUIRES_FEEDBACK',
                confirmation_prompted_at = NOW(),
                updated_at = NOW()
            WHERE status = 'CONFIRMED'
              AND deleted_at IS NULL
              AND scheduled_end_utc_time < NOW()
              AND confirmation_prompted_at IS NULL
            """, nativeQuery = true)
    int transitionPastConfirmedToRequiresFeedback();

    /**
     * Auto-complete schedules that have been awaiting feedback for more than 48 hours.
     * Uses the exact hardcoded interval from the architecture plan.
     */
    @Modifying
    @Query(value = """
            UPDATE viewing_schedules
            SET status = 'COMPLETED',
                completed_at = NOW(),
                updated_at = NOW()
            WHERE status = 'REQUIRES_FEEDBACK'
              AND deleted_at IS NULL
              AND confirmation_prompted_at < NOW() - INTERVAL '48 hours'
            """, nativeQuery = true)
    int autoCompleteStaleFeedbackRequests();
}
