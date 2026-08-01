package com.yoedu.yoedurealestateapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.frontend")
public record AppFrontendConfig(
    @NotBlank String url
) {
}
