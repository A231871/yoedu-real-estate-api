package com.yoedu.yoedurealestateapi.dto.view_schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelViewingScheduleRequest {

    @NotBlank(message = "Cancel reason must not be blank")
    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    private String reason;
}
