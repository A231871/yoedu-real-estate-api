package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Favorite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUser_IdAndListing_Id(UUID userId, UUID listingId);

    Optional<Favorite> findByUser_IdAndListing_Id(UUID userId, UUID listingId);

    @Modifying
    int deleteByUser_IdAndListing_Id(UUID userId, UUID listingId);

    @EntityGraph(attributePaths = {
            "listing",
            "listing.owner",
            "listing.agent",
            "listing.ward",
            "listing.ward.province",
            "listing.listingMedias",
            "listing.prices"
    })
    Page<Favorite> findByUser_IdAndListing_DeletedAtIsNullOrderByCreatedAtDesc(
            UUID userId, Pageable pageable);
}
