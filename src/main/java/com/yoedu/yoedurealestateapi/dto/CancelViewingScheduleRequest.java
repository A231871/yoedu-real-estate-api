package com.yoedu.yoedurealestateapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for the Host cancel endpoint.
 * Intentionally minimal — only a cancel reason is needed.
 */
@Getter
@Setter
public class CancelViewingScheduleRequest {

    @NotBlank(message = "Cancel reason must not be blank")
    @Size(max = 500, message = "Cancel reason must not exceed 500 characters")
    private String reason;
}
