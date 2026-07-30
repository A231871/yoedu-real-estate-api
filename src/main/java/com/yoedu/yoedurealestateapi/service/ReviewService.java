package com.yoedu.yoedurealestateapi.service;

import com.yoedu.yoedurealestateapi.dto.review.CreateReportRequest;
import com.yoedu.yoedurealestateapi.dto.review.CreateReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReplyToReviewRequest;
import com.yoedu.yoedurealestateapi.dto.review.ReviewResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

  ReviewResponse createReview(CreateReviewRequest request, UUID reviewerId);

  Page<ReviewResponse> getReviewsForListing(UUID listingId, Pageable pageable);

  Page<ReviewResponse> getReviewsForHost(UUID hostId, Pageable pageable);

  Page<ReviewResponse> getFeedbackForListing(UUID listingId, Pageable pageable);

  ReviewResponse replyToReview(UUID reviewId, ReplyToReviewRequest request, UUID hostId);

  void reportListing(CreateReportRequest request, UUID reporterId);
}
