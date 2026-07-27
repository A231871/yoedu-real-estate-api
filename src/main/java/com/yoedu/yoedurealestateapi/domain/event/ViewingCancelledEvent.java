package com.yoedu.yoedurealestateapi.domain.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewingCancelledEvent {
    private UUID scheduleId;
    private UUID listingId;
    private UUID clientId;
    private UUID hostId;
    private UUID cancelledBy;
    private String reason;
}
