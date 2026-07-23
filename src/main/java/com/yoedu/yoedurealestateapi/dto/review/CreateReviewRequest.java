package com.yoedu.yoedurealestateapi.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewRequest {

  /** The listing being reviewed. */
  @NotNull(message = "listingId is required")
  private UUID listingId;

  /**
   * The COMPLETED viewing schedule that qualifies the reviewer.
   * The backend verifies that this schedule belongs to the reviewer and the listing,
   * and that its status is COMPLETED.
   */
  @NotNull(message = "scheduleId is required — a completed viewing is needed to leave a review")
  private UUID scheduleId;

  /** Overall star rating, 1–5. */
  @NotNull(message = "rating is required")
  @Min(value = 1, message = "rating must be at least 1")
  @Max(value = 5, message = "rating must be at most 5")
  private Short rating;

  /** Optional sub-rating for the location, 1–5. */
  @Min(value = 1, message = "locationRating must be at least 1")
  @Max(value = 5, message = "locationRating must be at most 5")
  private Short locationRating;

  /** Optional sub-rating for listing accuracy, 1–5. */
  @Min(value = 1, message = "accuracyRating must be at least 1")
  @Max(value = 5, message = "accuracyRating must be at most 5")
  private Short accuracyRating;

  /** Optional sub-rating for the host, 1–5. */
  @Min(value = 1, message = "hostRating must be at least 1")
  @Max(value = 5, message = "hostRating must be at most 5")
  private Short hostRating;

  /** Optional short headline for the review. */
  @Size(max = 200, message = "title must be at most 200 characters")
  private String title;

  /** Main review body. Required. */
  @NotBlank(message = "comment is required")
  @Size(max = 5000, message = "comment must be at most 5000 characters")
  private String comment;
}
