package com.yoedu.yoedurealestateapi.controller;

import com.yoedu.yoedurealestateapi.common.ApiResponse;
import com.yoedu.yoedurealestateapi.dto.review.CreateReportRequest;
import com.yoedu.yoedurealestateapi.dto.review.CreateReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReplyToReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReviewResponse;
import com.yoedu.yoedurealestateapi.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "APIs for creating and reading verified listing reviews")
public class ReviewController {

  private final ReviewService reviewService;

  // ──────────────────────────────────────────────────────────────────────────
  // Reviews
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Creates a review for a listing. The requesting user must have a {@code COMPLETED}
   * viewing schedule for the specified listing to prevent fake reviews.
   */
  @PostMapping("/reviews")
  @Operation(
      summary = "Create a verified review",
      description = "Submit a review for a listing. "
          + "A COMPLETED viewing schedule for that listing is required.")
  public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
      @Valid @RequestBody CreateReviewRequest request,
      Authentication authentication) {

    UUID reviewerId = UUID.fromString(authentication.getName());
    ReviewResponse response = reviewService.createReview(request, reviewerId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Review submitted successfully.", response));
  }

  /**
   * Returns paginated reviews. Exactly one of {@code listingId} or {@code hostId}
   * must be provided as a query parameter.
   */
  @GetMapping("/reviews")
  @Operation(
      summary = "List reviews",
      description = "Retrieve paginated reviews filtered by listingId or hostId. "
          + "Provide exactly one of the two query parameters.")
  public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
      @Parameter(description = "Filter reviews by listing ID")
      @RequestParam(required = false) UUID listingId,
      @Parameter(description = "Filter reviews by host user ID")
      @RequestParam(required = false) UUID hostId,
      Pageable pageable) {

    if (listingId != null) {
      return ResponseEntity.ok(ApiResponse.success(
          reviewService.getReviewsForListing(listingId, pageable)));
    }
    if (hostId != null) {
      return ResponseEntity.ok(ApiResponse.success(
          reviewService.getReviewsForHost(hostId, pageable)));
    }
    return ResponseEntity.badRequest().body(
        new ApiResponse<>(false, "Provide either 'listingId' or 'hostId' as a query parameter.", null, java.time.Instant.now())
    );
  }

  /**
   * Returns paginated feedback (reviews) for a specific listing.
   * Dedicated endpoint per the spec — semantically equivalent to
   * GET /api/reviews?listingId={listingId}.
   */
  @GetMapping("/reviews/listing/{listingId}/feedback")
  @Operation(
      summary = "Get paginated feedback for a listing",
      description = "Returns all visible, non-hidden reviews for the given listing, paginated.")
  public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getListingFeedback(
      @PathVariable UUID listingId,
      Pageable pageable) {

    Page<ReviewResponse> page = reviewService.getFeedbackForListing(listingId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Feedback retrieved successfully.", page));
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Host reply
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Allows the host of the reviewed listing to post a reply.
   * Access is restricted via {@code @reviewSecurity.isListingHostForReview(#reviewId)}.
   */
  @PostMapping("/reviews/{reviewId}/reply")
  @PreAuthorize("@reviewSecurity.isListingHostForReview(#reviewId)")
  @Operation(
      summary = "Host replies to a review",
      description = "The host of the listing may post a single reply to a review. "
          + "Only the listing host is authorised. Editing an existing reply is not supported.")
  public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
      @PathVariable UUID reviewId,
      @Valid @RequestBody ReplyToReviewRequest request,
      Authentication authentication) {

    UUID hostId = UUID.fromString(authentication.getName());
    ReviewResponse response = reviewService.replyToReview(reviewId, request, hostId);
    return ResponseEntity.ok(ApiResponse.success("Reply posted successfully.", response));
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Reports (Dev 3 boundary — event only)
  // ──────────────────────────────────────────────────────────────────────────

  /**
   * Any authenticated user can submit a report for a listing.
   * Dev 2 publishes a {@code ListingReportedEvent}; Dev 3's listener persists the record.
   */
  @PostMapping("/reports")
  @Operation(
      summary = "Report a listing",
      description = "Submit a fraud / inaccuracy / inappropriate-content report for a listing. "
          + "Allowed reasons: FRAUD, DUPLICATE, WRONG_INFO, INAPPROPRIATE_CONTENT, "
          + "WRONG_PRICE, ALREADY_RENTED, OTHER.")
  public ResponseEntity<ApiResponse<Void>> reportListing(
      @Valid @RequestBody CreateReportRequest request,
      Authentication authentication) {

    UUID reporterId = UUID.fromString(authentication.getName());
    reviewService.reportListing(request, reporterId);
    return ResponseEntity.accepted()
        .body(ApiResponse.successMessage("Report submitted. Thank you for keeping the platform safe."));
  }
}
