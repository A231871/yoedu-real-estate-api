package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.dto.UpsertViewingScheduleRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Service trả về entity, Controller lo việc map sang DTO. */
public interface ViewingScheduleService {

    ViewingSchedule createSchedule(UpsertViewingScheduleRequest request, UUID clientId);

    ViewingSchedule confirmSchedule(UUID id);

    ViewingSchedule cancelSchedule(UUID id, String reason, UUID actorId);

    ViewingSchedule getScheduleById(UUID id);

    Page<ViewingSchedule> getHostSchedules(UUID hostId, List<String> statuses, Pageable pageable);

    Page<ViewingSchedule> getClientSchedules(UUID clientId, List<String> statuses, Pageable pageable);
}
