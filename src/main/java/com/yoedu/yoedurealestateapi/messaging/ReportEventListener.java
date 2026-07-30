package com.yoedu.yoedurealestateapi.messaging;

import com.yoedu.yoedurealestateapi.domain.events.ListingReportedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportEventListener {

  /**
   * Receives a {@link ListingReportedEvent} published by Dev 2's review service.
   * Currently only logs the event. Dev 3 will replace this with DB persistence.
   *
   * @param event the published report event
   */
  @Async
  @EventListener
  public void onListingReported(ListingReportedEvent event) {
    log.info(
        "[STUB] ListingReportedEvent received — listingId={}, reporterId={}, reason={}. "
            + "Dev 3 must replace this stub with real persistence.",
        event.listingId(),
        event.reporterId(),
        event.reason()
    );
  }
}
