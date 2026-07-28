package com.yoedu.yoedurealestateapi.domain.listings.listener;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspendedEvent;
import com.yoedu.yoedurealestateapi.domain.event.ListingSuspensionRequestedEvent;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modulith listener residing strictly inside the listings domain layer.
 * Listens for asynchronous {@link ListingSuspensionRequestedEvent} emitted by the Admin domain,
 * handles the database state update to SUSPENDED, and emits a post-suspension {@link ListingSuspendedEvent}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListingSuspensionListener {

    private final ListingRepository listingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleListingSuspensionRequested(ListingSuspensionRequestedEvent event) {
        log.info("Handling ListingSuspensionRequestedEvent for listingId: {} by admin: {}",
            event.getListingId(), event.getAdminId());

        Optional<Listing> listingOpt = listingRepository.findWithOwnerByIdAndDeletedAtIsNull(event.getListingId());
        if (listingOpt.isEmpty()) {
            log.warn("Listing {} not found or already deleted. Skipping suspension.", event.getListingId());
            return;
        }

        Listing listing = listingOpt.get();
        if (listing.getStatus() == ListingStatus.SUSPENDED) {
            log.info("Listing {} is already SUSPENDED. Skipping duplicate suspension.", listing.getId());
            return;
        }

        listing.setStatus(ListingStatus.SUSPENDED);
        listingRepository.save(listing);

        eventPublisher.publishEvent(ListingSuspendedEvent.builder()
            .listingId(listing.getId())
            .listingTitle(listing.getTitle())
            .ownerId(listing.getOwner() != null ? listing.getOwner().getId() : null)
            .adminId(event.getAdminId())
            .reason(event.getReason())
            .build());
    }
}
