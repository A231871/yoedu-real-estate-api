package com.yoedu.yoedurealestateapi.security;

import com.yoedu.yoedurealestateapi.domain.entities.Review;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.repository.ReviewRepository;
import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("reviewSecurity")
@RequiredArgsConstructor
public class ReviewSecurity {

  private final ReviewRepository reviewRepository;
  private final ViewingScheduleRepository viewingScheduleRepository;

  /**
   * Returns {@code true} if the current authenticated user is the host of the listing
   * associated with the given review.
   *
   * <p>Used to guard the "POST /api/reviews/{reviewId}/reply" endpoint so that only
   * the host of the reviewed listing can post a reply.
   *
   * @param reviewId the ID of the review to check ownership for
   * @return {@code true} if the current user is the host; {@code false} otherwise
   */
  @Transactional(readOnly = true)
  public boolean isListingHostForReview(UUID reviewId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      return false;
    }

    UUID userId;
    try {
      userId = UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException e) {
      return false;
    }

    Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId).orElse(null);
    if (review == null || review.getScheduleId() == null) {
      return false;
    }

    ViewingSchedule schedule = viewingScheduleRepository
        .findByIdAndDeletedAtIsNull(review.getScheduleId())
        .orElse(null);
    if (schedule == null) {
      return false;
    }

    return userId.equals(schedule.getHostId());
  }
}
