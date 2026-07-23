package com.yoedu.yoedurealestateapi.dto.review;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewResponse {

  private UUID id;
  private UUID listingId;
  private UUID reviewerId;
  private UUID scheduleId;
  private Short rating;
  private Short locationRating;
  private Short accuracyRating;
  private Short hostRating;
  private String title;
  private String comment;
  private boolean verified;
  private boolean hidden;
  private String ownerReply;
  private Instant ownerRepliedAt;
  private Instant createdAt;
  private Instant updatedAt;
}
