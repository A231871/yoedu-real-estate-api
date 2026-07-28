package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.Report;
import com.yoedu.yoedurealestateapi.domain.enums.ReportStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    @EntityGraph(attributePaths = {"listing", "listing.owner", "listing.propertyType", "listing.prices", "reporter", "resolvedBy"})
    Page<Report> findByStatusAndDeletedAtIsNull(ReportStatus status, Pageable pageable);
}
