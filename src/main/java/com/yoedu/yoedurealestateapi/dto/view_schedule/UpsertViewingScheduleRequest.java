package com.yoedu.yoedurealestateapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertViewingScheduleRequest {

    @NotNull(message = "Listing id must not be blank")
    private UUID listingId;

    @NotNull(message = "Scheduled local time must not be blank")
    private LocalDateTime scheduledLocalTime;

    @NotBlank(message = "Timezone id must not be blank")
    private String timezoneId;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
    
    @NotBlank(message = "Cancel reason must not be blank")
    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    private String reason;
}
    
    
