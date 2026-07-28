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
public class ListingSuspensionRequestedEvent {
    private UUID listingId;
    private UUID adminId;
    private String reason;
}
