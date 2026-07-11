package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import com.yoedu.yoedurealestateapi.service.ViewingScheduleService;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service thuần business logic, trả về entity.
 * Không phụ thuộc ModelMapper — Controller sẽ lo việc map sang DTO.
 */
@Service
@RequiredArgsConstructor
public class ViewingScheduleServiceImpl implements ViewingScheduleService {

    private final ViewingScheduleRepository viewingScheduleRepository;
    private final ListingRepository listingRepository;

    @Override
    @Transactional
    public ViewingSchedule createSchedule(UpsertViewingScheduleRequest request, UUID clientId) {
        Listing listing = listingRepository.findById(request.getListingId())
                .filter(l -> l.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tin đăng hoặc tin đăng đã bị xóa"));

        if (!"APPROVED".equals(listing.getStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể đặt lịch hẹn cho các tin đăng đã được phê duyệt");
        }

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(request.getTimezoneId());
        } catch (DateTimeException e) {
            throw new BadRequestException(
                    "Múi giờ không hợp lệ: " + request.getTimezoneId());
        }

        ZonedDateTime localZonedDateTime = request.getScheduledLocalTime().atZone(zoneId);
        Instant utcStart = localZonedDateTime.toInstant();

        if (utcStart.isBefore(Instant.now())) {
            throw new BadRequestException("Không thể đặt lịch hẹn trong quá khứ");
        }

        int duration = (request.getDurationMinutes() != null)
                ? request.getDurationMinutes()
                : 60;
        if (duration <= 0) {
            throw new BadRequestException("Thời lượng lịch hẹn phải lớn hơn 0");
        }
        Instant utcEnd = localZonedDateTime.plusMinutes(duration).toInstant();

        ViewingSchedule schedule = new ViewingSchedule();
        schedule.setListingId(listing.getId());
        schedule.setClientId(clientId);

        UUID hostId = (listing.getAgentId() != null)
                ? listing.getAgentId()
                : listing.getOwnerId();
        if (clientId.equals(hostId)) {
            throw new BadRequestException(
                    "Khách thuê và Chủ nhà/Môi giới không được phép trùng nhau");
        }
        schedule.setHostId(hostId);

        schedule.setScheduledLocalTime(request.getScheduledLocalTime());
        schedule.setTimezoneId(zoneId.getId());
        schedule.setScheduledStart(utcStart);
        schedule.setScheduledEnd(utcEnd);
        schedule.setNote(request.getNote());
        schedule.setStatus("PENDING");
        schedule.setReminderSent(false);

        return viewingScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public ViewingSchedule confirmSchedule(UUID id) {
        ViewingSchedule schedule = getActiveSchedule(id);

        if (!"PENDING".equals(schedule.getStatus())) {
            throw new BadRequestException(
                    "Lịch hẹn hiện tại không ở trạng thái PENDING.");
        }

        schedule.setStatus("CONFIRMED");
        schedule.setConfirmedAt(Instant.now());

        return viewingScheduleRepository.save(schedule);
    }

    @Override
    @Transactional
    public ViewingSchedule cancelSchedule(UUID id, String reason, UUID actorId) {
        ViewingSchedule schedule = getActiveSchedule(id);

        if ("CANCELLED".equals(schedule.getStatus())) {
            throw new BadRequestException("Lịch hẹn này đã bị hủy từ trước");
        }
        if ("COMPLETED".equals(schedule.getStatus())) {
            throw new BadRequestException("Không thể hủy lịch hẹn đã hoàn thành");
        }

        schedule.setStatus("CANCELLED");
        schedule.setCancelReason(reason);
        schedule.setCancelledBy(actorId);
        schedule.setCancelledAt(Instant.now());

        return viewingScheduleRepository.save(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public ViewingSchedule getScheduleById(UUID id) {
        return getActiveSchedule(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingSchedule> getHostSchedules(
            UUID hostId, List<String> statuses, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            return viewingScheduleRepository
                    .findByHostIdAndDeletedAtIsNullOrderByScheduledStartDesc(hostId, pageable);
        }
        return viewingScheduleRepository
                .findByHostIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
                        hostId, statuses, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingSchedule> getClientSchedules(
            UUID clientId, List<String> statuses, Pageable pageable) {
        if (statuses == null || statuses.isEmpty()) {
            return viewingScheduleRepository
                    .findByClientIdAndDeletedAtIsNullOrderByScheduledStartDesc(clientId, pageable);
        }
        return viewingScheduleRepository
                .findByClientIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
                        clientId, statuses, pageable);
    }

    private ViewingSchedule getActiveSchedule(UUID id) {
        return viewingScheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy lịch hẹn hoặc lịch hẹn đã bị xóa"));
    }
}