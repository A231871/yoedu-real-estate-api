package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @EntityGraph(attributePaths = {"actor"})
    Page<AuditLog> findByActorId(UUID actorId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor"})
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"actor"})
    Page<AuditLog> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"actor"})
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}
