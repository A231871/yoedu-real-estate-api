package com.yoedu.yoedurealestateapi.scheduler;

import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewingScheduleCronJob {

    private final ViewingScheduleRepository viewingScheduleRepository;

    /**
     * Finds past CONFIRMED viewings (by indexed scheduled_end_utc_time) and prompts for feedback.
     */
    @Scheduled(cron = "${app.viewing.cron.prompt-feedback:0 */15 * * * *}")
    @SchedulerLock(
            name = "viewingSchedulePromptFeedback",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT14M")
    @Transactional
    public void promptPastSchedulesForFeedback() {
        int updated = viewingScheduleRepository.transitionPastConfirmedToRequiresFeedback();
        if (updated > 0) {
            log.info("Transitioned {} past viewing schedule(s) to REQUIRES_FEEDBACK", updated);
        }
    }

    /**
     * Auto-completes REQUIRES_FEEDBACK schedules after the 48-hour feedback window expires.
     */
    @Scheduled(cron = "${app.viewing.cron.auto-complete:0 5 * * * *}")
    @SchedulerLock(
            name = "viewingScheduleAutoComplete",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT55M")
    @Transactional
    public void autoCompleteAfterFeedbackWindow() {
        int updated = viewingScheduleRepository.autoCompleteStaleFeedbackRequests();
        if (updated > 0) {
            log.info(
                    "Auto-completed {} viewing schedule(s) after 48-hour feedback window",
                    updated);
        }
    }
}
