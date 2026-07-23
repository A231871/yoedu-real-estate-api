package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Review;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
  Optional<Review> findByIdAndDeletedAtIsNull(UUID id);

  Page<Review> findByListingIdAndHiddenFalseAndDeletedAtIsNull(
      UUID listingId, Pageable pageable);
  /**
   * Paginated list of visible reviews for a host.
   * Traverses via the schedule_id FK to find all reviews where the viewing's host_id
   * matches the given hostId. Only non-hidden, non-deleted reviews are returned.
   */
  @Query("""
      SELECT r FROM Review r
      WHERE r.scheduleId IN (
          SELECT vs.id FROM ViewingSchedule vs
          WHERE vs.hostId = :hostId
            AND vs.deletedAt IS NULL
      )
      AND r.hidden = FALSE
      AND r.deletedAt IS NULL
      """)
  Page<Review> findByHostId(@Param("hostId") UUID hostId, Pageable pageable);


  boolean existsByScheduleIdAndDeletedAtIsNull(UUID scheduleId);

  boolean existsByListingIdAndReviewerIdAndDeletedAtIsNull(UUID listingId, UUID reviewerId);
}
