package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.view_schedule.CreateViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.view_schedule.ViewingScheduleResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ViewingScheduleService {

    ViewingScheduleResponse createSchedule(CreateViewingScheduleRequest request, UUID clientId);

    ViewingScheduleResponse confirmSchedule(UUID id);

    ViewingScheduleResponse cancelSchedule(UUID id, String reason, UUID actorId);

    ViewingScheduleResponse getScheduleById(UUID id);

    Page<ViewingScheduleResponse> getHostSchedules(UUID hostId, List<String> statuses, Pageable pageable);

    Page<ViewingScheduleResponse> getClientSchedules(UUID clientId, List<String> statuses, Pageable pageable);
}
