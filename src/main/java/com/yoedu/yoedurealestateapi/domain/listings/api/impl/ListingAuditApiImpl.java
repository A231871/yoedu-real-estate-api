package com.yoedu.yoedurealestateapi.domain.listings.api.impl;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.listings.api.ListingAuditApi;
import com.yoedu.yoedurealestateapi.dto.moderation.ListingAuditHistoryResponse;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingAuditApiImpl implements ListingAuditApi {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<ListingAuditHistoryResponse> getListingAuditHistory(UUID listingId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<Object[]> results = auditReader.createQuery()
            .forRevisionsOfEntity(Listing.class, false, true)
            .add(AuditEntity.id().eq(listingId))
            .addOrder(AuditEntity.revisionNumber().asc())
            .getResultList();

        List<ListingAuditHistoryResponse> history = new ArrayList<>();
        for (Object[] row : results) {
            // row[0] is null for DEL (deletion) revisions — guard required
            Listing listingSnapshot = (Listing) row[0];
            DefaultRevisionEntity revEntity = (DefaultRevisionEntity) row[1];
            RevisionType revType = (RevisionType) row[2];

            Instant revisedAt = Instant.ofEpochMilli(revEntity.getTimestamp());

            // For deletion revisions, the snapshot is null — use safe fallback values
            UUID snapshotId = listingSnapshot != null ? listingSnapshot.getId() : listingId;
            String title = listingSnapshot != null ? listingSnapshot.getTitle() : null;
            String status = (listingSnapshot != null && listingSnapshot.getStatus() != null)
                ? listingSnapshot.getStatus().name()
                : null;

            // prices is @NotAudited — accessing it on an Envers snapshot triggers
            // LazyInitializationException. Price is intentionally omitted here.

            history.add(new ListingAuditHistoryResponse(
                revEntity.getId(),
                revisedAt,
                revType.name(),
                snapshotId,
                title,
                status
            ));
        }

        return history;
    }
}
