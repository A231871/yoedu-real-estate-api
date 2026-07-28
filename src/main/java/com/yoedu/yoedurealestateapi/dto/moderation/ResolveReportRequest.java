package com.yoedu.yoedurealestateapi.dto.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /admin/moderation/reports/{id}/resolve.
 *
 * @param resolution  Must be "RESOLVED" or "DISMISSED".
 * @param adminNote   Optional internal note (max 500 chars) recorded on the report.
 * @param suspendListing When resolution is "RESOLVED", instructs the service to cascade
 *                       a SUSPENDED status onto the reported listing.
 */
public record ResolveReportRequest(
    @NotBlank(message = "resolution is required")
    @Pattern(regexp = "^(?i)(RESOLVED|DISMISSED)$", message = "resolution must be RESOLVED or DISMISSED")
    String resolution,

    @Size(max = 500, message = "adminNote must not exceed 500 characters")
    String adminNote,

    @NotNull(message = "suspendListing must be provided")
    Boolean suspendListing
) {}
