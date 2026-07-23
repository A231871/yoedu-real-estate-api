package com.yoedu.yoedurealestateapi.dto.review;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

  @NotNull(message = "listingId is required")
  private UUID listingId;

  /**
   * Reason category for the report. Must be one of:
   * FRAUD, DUPLICATE, WRONG_INFO, INAPPROPRIATE_CONTENT, WRONG_PRICE, ALREADY_RENTED, OTHER.
   */
  @NotNull(message = "reason is required")
  private String reason;

  @Size(max = 2000, message = "description must be at most 2000 characters")
  private String description;

  /** URLs to supporting evidence (screenshots, documents). Maximum 10 items. */
  @Size(max = 10, message = "evidenceUrls may contain at most 10 items")
  private List<String> evidenceUrls;
}
