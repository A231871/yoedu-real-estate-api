package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
        UUID hostId, List<String> status, Pageable pageable);

    // GET /requested — không lọc status
    Page<ViewingSchedule> findByClientIdAndDeletedAtIsNullOrderByScheduledStartDesc(
        UUID clientId, Pageable pageable);

    // GET /requested?status=CONFIRMED — lọc theo status
    Page<ViewingSchedule> findByClientIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
        UUID clientId, List<String> status, Pageable pageable);
}
