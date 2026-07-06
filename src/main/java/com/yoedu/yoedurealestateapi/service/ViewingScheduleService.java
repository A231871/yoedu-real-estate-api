package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import com.yoedu.yoedurealestateapi.dto.ViewingScheduleResponse;
import java.util.UUID;

public interface ViewingScheduleService {
    ViewingScheduleResponse createSchedule(UpsertViewingScheduleRequest request, UUID clientId);
    ViewingScheduleResponse confirmSchedule(UUID id);
    ViewingScheduleResponse cancelSchedule(UUID id, String reason, UUID actorId);
}