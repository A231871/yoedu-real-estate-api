package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Review;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.domain.enums.ViewingScheduleStatus;
import com.yoedu.yoedurealestateapi.domain.events.ListingReportedEvent;
import com.yoedu.yoedurealestateapi.dto.review.CreateReportRequest;
import com.yoedu.yoedurealestateapi.dto.review.CreateReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReplyToReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReviewResponse;
import com.yoedu.yoedurealestateapi.repository.ReviewRepository;
import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import com.yoedu.yoedurealestateapi.service.ReviewService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final ViewingScheduleRepository viewingScheduleRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public ReviewResponse createReview(CreateReviewRequest request, UUID reviewerId) {
    ViewingSchedule schedule = viewingScheduleRepository
        .findByIdAndDeletedAtIsNull(request.getScheduleId())
        .orElseThrow(() -> new NotFoundException(
            "Viewing schedule not found or has been deleted."));

    if (!ViewingScheduleStatus.COMPLETED.equals(schedule.getStatus())) {
      throw new BadRequestException(
          "A review can only be submitted after the viewing schedule is COMPLETED. "
              + "Current status: " + schedule.getStatus());
    }

    if (!reviewerId.equals(schedule.getClientId())) {
      throw new BadRequestException(
          "You can only review a viewing schedule that you attended as a renter.");
    }

    if (!request.getListingId().equals(schedule.getListingId())) {
      throw new BadRequestException(
          "The provided viewing schedule does not belong to the specified listing.");
    }

    if (reviewerId.equals(schedule.getHostId())) {
      throw new BadRequestException("A host cannot review their own listing.");
    }

    if (reviewRepository.existsByScheduleIdAndDeletedAtIsNull(request.getScheduleId())) {
      throw new BadRequestException(
          "A review for this viewing schedule has already been submitted.");
    }

    if (reviewRepository.existsByListingIdAndReviewerIdAndDeletedAtIsNull(
        request.getListingId(), reviewerId)) {
      throw new BadRequestException("You have already reviewed this listing.");
    }

    Review review = new Review();
    review.setListingId(request.getListingId());
    review.setReviewerId(reviewerId);
    review.setScheduleId(request.getScheduleId());
    review.setRating(request.getRating());
    review.setLocationRating(request.getLocationRating());
    review.setAccuracyRating(request.getAccuracyRating());
    review.setHostRating(request.getHostRating());
    review.setTitle(request.getTitle());
    review.setComment(request.getComment());
    review.setVerified(true);
    review.setHidden(false);

    return toDto(reviewRepository.save(review));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReviewResponse> getReviewsForListing(UUID listingId, Pageable pageable) {
    return reviewRepository
        .findByListingIdAndHiddenFalseAndDeletedAtIsNull(listingId, pageable)
        .map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReviewResponse> getReviewsForHost(UUID hostId, Pageable pageable) {
    return reviewRepository.findByHostId(hostId, pageable).map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ReviewResponse> getFeedbackForListing(UUID listingId, Pageable pageable) {
    return getReviewsForListing(listingId, pageable);
  }

  @Override
  @Transactional
  public ReviewResponse replyToReview(
      UUID reviewId, ReplyToReviewRequest request, UUID hostId) {

    Review review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
        .orElseThrow(() -> new NotFoundException("Review not found."));

    if (review.getOwnerReply() != null) {
      throw new BadRequestException(
          "A reply has already been posted for this review. "
              + "Edit functionality is not supported in Phase 1.");
    }

    review.setOwnerReply(request.getReply());
    review.setOwnerRepliedAt(Instant.now());

    return toDto(reviewRepository.save(review));
  }

  @Override
  public void reportListing(CreateReportRequest request, UUID reporterId) {
    validateReportReason(request.getReason());

    List<String> evidenceUrls = request.getEvidenceUrls() != null
        ? List.copyOf(request.getEvidenceUrls())
        : List.of();

    eventPublisher.publishEvent(new ListingReportedEvent(
        request.getListingId(),
        reporterId,
        request.getReason(),
        request.getDescription(),
        evidenceUrls));
  }

  private ReviewResponse toDto(Review entity) {
    ReviewResponse dto = new ReviewResponse();
    dto.setId(entity.getId());
    dto.setListingId(entity.getListingId());
    dto.setReviewerId(entity.getReviewerId());
    dto.setScheduleId(entity.getScheduleId());
    dto.setRating(entity.getRating());
    dto.setLocationRating(entity.getLocationRating());
    dto.setAccuracyRating(entity.getAccuracyRating());
    dto.setHostRating(entity.getHostRating());
    dto.setTitle(entity.getTitle());
    dto.setComment(entity.getComment());
    dto.setVerified(entity.isVerified());
    dto.setHidden(entity.isHidden());
    dto.setOwnerReply(entity.getOwnerReply());
    dto.setOwnerRepliedAt(entity.getOwnerRepliedAt());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setUpdatedAt(entity.getUpdatedAt());
    return dto;
  }

  private void validateReportReason(String reason) {
    List<String> allowed = List.of(
        "FRAUD", "DUPLICATE", "WRONG_INFO",
        "INAPPROPRIATE_CONTENT", "WRONG_PRICE", "ALREADY_RENTED", "OTHER");
    if (!allowed.contains(reason)) {
      throw new BadRequestException(
          "Invalid report reason. Allowed values: " + allowed);
    }
  }
}
