package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review extends AuditableEntity {


  @Column(name = "listing_id", nullable = false)
  private UUID listingId;

  @Column(name = "reviewer_id")
  private UUID reviewerId;

  @Column(name = "schedule_id")
  private UUID scheduleId;

  @Column(name = "rating", nullable = false)
  private Short rating;

  @Column(name = "location_rating")
  private Short locationRating;

  @Column(name = "accuracy_rating")
  private Short accuracyRating;

  @Column(name = "host_rating")
  private Short hostRating;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
  private String comment;

  @Column(name = "is_verified", nullable = false)
  private boolean verified;

  @Column(name = "is_hidden", nullable = false)
  private boolean hidden;

  @Column(name = "owner_reply", columnDefinition = "TEXT")
  private String ownerReply;

  @Column(name = "owner_replied_at")
  private Instant ownerRepliedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;
}
