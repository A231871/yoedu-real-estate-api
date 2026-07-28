package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    Optional<Listing> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"owner", "propertyType", "prices"})
    Page<Listing> findByStatusAndDeletedAtIsNull(ListingStatus status, Pageable pageable);
}
