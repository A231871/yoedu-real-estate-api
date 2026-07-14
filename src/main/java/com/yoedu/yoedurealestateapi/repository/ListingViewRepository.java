package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.ListingView;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingViewRepository extends JpaRepository<ListingView, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO listing_views (id, listing_id, user_id, ip_address, user_agent, viewed_at)
            VALUES (
                uuidv7(),
                CAST(:listingId AS UUID),
                :userId,
                CAST(:ipAddress AS inet),
                :userAgent,
                now()
            )
            """, nativeQuery = true)
    int insertView(
            @Param("listingId") UUID listingId,
            @Param("userId") UUID userId,
            @Param("ipAddress") String ipAddress,
            @Param("userAgent") String userAgent);

    /**
     * Fallback rows when buffered event metadata is missing (no IP — rare edge case).
     */
    @Modifying
    @Query(value = """
            INSERT INTO listing_views (id, listing_id, viewed_at)
            SELECT uuidv7(), CAST(:listingId AS UUID), now()
            FROM generate_series(1, :count)
            """, nativeQuery = true)
    int insertBufferedViews(@Param("listingId") UUID listingId, @Param("count") int count);
}
