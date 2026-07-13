package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.ViewingScheduleResponse;
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
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ViewingScheduleServiceImpl implements ViewingScheduleService {

    private final ViewingScheduleRepository viewingScheduleRepository;
    private final ListingRepository listingRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public ViewingScheduleResponse createSchedule(UpsertViewingScheduleRequest request, UUID clientId) {
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

        ViewingSchedule saved = viewingScheduleRepository.save(schedule);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ViewingScheduleResponse confirmSchedule(UUID id) {
        ViewingSchedule schedule = getActiveSchedule(id);

        if (!"PENDING".equals(schedule.getStatus())) {
            throw new BadRequestException(
                    "Lịch hẹn hiện tại không ở trạng thái PENDING.");
        }

        schedule.setStatus("CONFIRMED");
        schedule.setConfirmedAt(Instant.now());

        ViewingSchedule saved = viewingScheduleRepository.save(schedule);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ViewingScheduleResponse cancelSchedule(UUID id, String reason, UUID actorId) {
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
                            hostId, statuses, pageable);
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
                            clientId, statuses, pageable);
        }
        return page.map(this::toDto);
    }

    private ViewingSchedule getActiveSchedule(UUID id) {
        return viewingScheduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy lịch hẹn hoặc lịch hẹn đã bị xóa"));
    }

    private ViewingScheduleResponse toDto(ViewingSchedule entity) {
        ViewingScheduleResponse dto = modelMapper.map(entity, ViewingScheduleResponse.class);
        dto.setScheduledUtcTime(entity.getScheduledStart());
        dto.setScheduledEndUtcTime(entity.getScheduledEnd());
        return dto;
    }
}