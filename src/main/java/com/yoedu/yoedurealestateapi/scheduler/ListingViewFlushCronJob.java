package com.yoedu.yoedurealestateapi.scheduler;

import com.yoedu.yoedurealestateapi.service.ListingViewFlushService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingViewFlushCronJob {

    private final ListingViewFlushService flushService;

    @Scheduled(cron = "${app.listing-views.cron.flush:0 */5 * * * *}")
    @SchedulerLock(
            name = "listingViewFlush",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT4M")
    public void flushBufferedViews() {
        flushService.flushAll();
    }
}
