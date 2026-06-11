package com.yoedu.yoedurealestateapi.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record AppJwtProperties(
    @NotBlank String issuer,
    @NotBlank String secret,
    @Min(1) long accessTokenTtlMinutes,
    @Min(1) long refreshTokenTtlDays
    ) {}
