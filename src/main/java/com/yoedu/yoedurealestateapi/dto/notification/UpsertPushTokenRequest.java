package com.yoedu.yoedurealestateapi.dto.notification;

import com.yoedu.yoedurealestateapi.domain.enums.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertPushTokenRequest {

    @NotBlank(message = "Push token cannot be blank")
    @Size(max = 500, message = "Push token must not exceed 500 characters")
    private String token;

    @NotNull(message = "Platform cannot be null")
    private PushPlatform platform;

    @Size(max = 255, message = "Device ID must not exceed 255 characters")
    private String deviceId;
}
