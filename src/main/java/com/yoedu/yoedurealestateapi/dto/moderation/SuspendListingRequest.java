package com.yoedu.yoedurealestateapi.dto.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /admin/moderation/listings/{id}/suspend.
 *
 * @param reason  Mandatory reason for suspension (max 500 chars), recorded in AuditLog.
 */
public record SuspendListingRequest(
    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must not exceed 500 characters")
    String reason
) {}
