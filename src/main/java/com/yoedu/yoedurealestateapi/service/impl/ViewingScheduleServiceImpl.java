package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ViewingScheduleStatus;
import com.yoedu.yoedurealestateapi.dto.view_schedule.CreateViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.view_schedule.ViewingScheduleResponse;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import com.yoedu.yoedurealestateapi.service.ViewingScheduleService;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewingScheduleServiceImpl implements ViewingScheduleService {

    private final ViewingScheduleRepository viewingScheduleRepository;
    private final ListingRepository listingRepository;

    @Value("${app.viewing.buffer-minutes:15}")
    private int bufferMinutes;

    @Override
    @Transactional
    public ViewingScheduleResponse createSchedule(CreateViewingScheduleRequest request, UUID clientId) {
        Listing listing = listingRepository.findByIdAndDeletedAtIsNull(request.getListingId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy tin đăng hoặc tin đăng đã bị xóa"));

        if (!ListingStatus.APPROVED.equals(listing.getStatus())) {
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
        int effectiveBuffer = Math.max(bufferMinutes, 0);
        Instant utcEnd = localZonedDateTime
                .plusMinutes(duration + (long) effectiveBuffer)
                .toInstant();

        ViewingSchedule schedule = new ViewingSchedule();
        schedule.setListingId(listing.getId());
        schedule.setClientId(clientId);

        UUID hostId = resolveHostId(listing);
        if (clientId.equals(hostId)) {
            throw new BadRequestException(
                    "Khách thuê và Chủ nhà/Môi giới không được phép trùng nhau");
        }
        schedule.setHostId(hostId);

        schedule.setScheduledLocalTime(request.getScheduledLocalTime());
        schedule.setTimezoneId(zoneId.getId());
        schedule.setScheduledStart(utcStart);
        schedule.setScheduledEnd(utcEnd);
        schedule.setScheduledUtcTime(utcStart);
        schedule.setScheduledEndUtcTime(utcEnd);
        schedule.setNote(request.getNote());
        schedule.setStatus(ViewingScheduleStatus.PENDING_CONFIRMATION);
        schedule.setReminderSent(false);

        ViewingSchedule saved = viewingScheduleRepository.save(schedule);
        return toDto(saved);
    }


    @Override
    @Transactional
    public ViewingScheduleResponse confirmSchedule(UUID id) {
        ViewingSchedule schedule = getActiveSchedule(id);

        if (!ViewingScheduleStatus.PENDING_CONFIRMATION.equals(schedule.getStatus())) {
            throw new BadRequestException(
                    "Lịch hẹn hiện tại không ở trạng thái PENDING_CONFIRMATION.");
        }

        schedule.setStatus(ViewingScheduleStatus.CONFIRMED);
        schedule.setConfirmedAt(Instant.now());

        ViewingSchedule saved = viewingScheduleRepository.save(schedule);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ViewingScheduleResponse cancelSchedule(UUID id, String reason, UUID actorId) {
        ViewingSchedule schedule = getActiveSchedule(id);

        if (ViewingScheduleStatus.CANCELLED.equals(schedule.getStatus())) {
            throw new BadRequestException("Lịch hẹn này đã bị hủy từ trước");
        }
        if (ViewingScheduleStatus.COMPLETED.equals(schedule.getStatus())) {
            throw new BadRequestException("Không thể hủy lịch hẹn đã hoàn thành");
        }

        schedule.setStatus(ViewingScheduleStatus.CANCELLED);
        schedule.setCancelReason(reason);
        schedule.setCancelledBy(actorId);
        schedule.setCancelledAt(Instant.now());

        ViewingSchedule saved = viewingScheduleRepository.save(schedule);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ViewingScheduleResponse getScheduleById(UUID id) {
        return toDto(getActiveSchedule(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingScheduleResponse> getHostSchedules(
            UUID hostId, List<String> statuses, Pageable pageable) {
        Page<ViewingSchedule> page;
        if (statuses == null || statuses.isEmpty()) {
            page = viewingScheduleRepository
                    .findByHostIdAndDeletedAtIsNullOrderByScheduledStartDesc(hostId, pageable);
        } else {
            page = viewingScheduleRepository
                    .findByHostIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
                            hostId, parseStatuses(statuses), pageable);
        }
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ViewingScheduleResponse> getClientSchedules(
            UUID clientId, List<String> statuses, Pageable pageable) {
        Page<ViewingSchedule> page;
        if (statuses == null || statuses.isEmpty()) {
            page = viewingScheduleRepository
                    .findByClientIdAndDeletedAtIsNullOrderByScheduledStartDesc(clientId, pageable);
        } else {
            page = viewingScheduleRepository
                    .findByClientIdAndStatusInAndDeletedAtIsNullOrderByScheduledStartDesc(
                            clientId, parseStatuses(statuses), pageable);
        }
        return page.map(this::toDto);
    }

    private List<ViewingScheduleStatus> parseStatuses(List<String> statuses) {
        try {
            return statuses.stream()
                    .map(value -> {
                        String normalized = value.trim().toUpperCase();
                        if ("PENDING".equals(normalized)) {
                            return ViewingScheduleStatus.PENDING_CONFIRMATION;
                        }
                        return ViewingScheduleStatus.valueOf(normalized);
                    })
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Status không hợp lệ. Hỗ trợ: "
                    + Arrays.toString(ViewingScheduleStatus.values()));
        }
    }

    private ViewingSchedule getActiveSchedule(UUID id) {
        return viewingScheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy lịch hẹn hoặc lịch hẹn đã bị xóa"));
    }

    private UUID resolveHostId(Listing listing) {
        User host = listing.getAgent() != null ? listing.getAgent() : listing.getOwner();
        if (host == null) {
            throw new BadRequestException("Listing does not have a valid host");
        }
        return host.getId();
    }

    private ViewingScheduleResponse toDto(ViewingSchedule entity) {
        ViewingScheduleResponse dto = new ViewingScheduleResponse();
        dto.setId(entity.getId());
        dto.setListingId(entity.getListingId());
        dto.setClientId(entity.getClientId());
        dto.setHostId(entity.getHostId());
        dto.setScheduledLocalTime(entity.getScheduledLocalTime());
        dto.setScheduledUtcTime(entity.getScheduledUtcTime());
        dto.setScheduledEndUtcTime(entity.getScheduledEndUtcTime());
        dto.setDurationMins(entity.getDurationMins());
        dto.setTimezoneId(entity.getTimezoneId());
        dto.setStatus(entity.getStatus().name());
        dto.setNote(entity.getNote());
        dto.setCancelReason(entity.getCancelReason());
        dto.setCancelledBy(entity.getCancelledBy());
        dto.setCancelledAt(entity.getCancelledAt());
        dto.setConfirmedAt(entity.getConfirmedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setReminderSent(entity.isReminderSent());
        dto.setVersion(entity.getVersion());
        return dto;
    }
}


