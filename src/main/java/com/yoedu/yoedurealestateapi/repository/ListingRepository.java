package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    Optional<Listing> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"owner", "propertyType", "prices"})
    Page<Listing> findByStatusAndDeletedAtIsNull(ListingStatus status, Pageable pageable);

    /** Eagerly loads owner for suspension/notification use-cases to prevent N+1 queries. */
    @EntityGraph(attributePaths = {"owner"})
    Optional<Listing> findWithOwnerByIdAndDeletedAtIsNull(UUID id);

    /** Fast projection query for high-frequency status polling endpoint. */
    @Query("SELECT l.status FROM Listing l WHERE l.id = :id AND l.deletedAt IS NULL")
    Optional<ListingStatus> findStatusById(@Param("id") UUID id);

    /** Used by AdminPropertyTypeServiceImpl to check before soft-deleting a property type. */
    boolean existsByPropertyTypeIdAndDeletedAtIsNull(Integer propertyTypeId);

    Page<Listing> findAllByListingType(ListingType listingType, Pageable pageable);
}
