package com.yoedu.yoedurealestateapi.security;

import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ViewingSchedule;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.ViewingScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("viewingScheduleSecurity")
@RequiredArgsConstructor
public class ViewingScheduleSecurity {

    private final ViewingScheduleRepository viewingScheduleRepository;
    private final ListingRepository listingRepository;

    @Transactional(readOnly = true)
    public boolean isListingOwner(UUID scheduleId) {
        UUID userId = currentUserId();
        if (userId == null) return false;

        ViewingSchedule schedule = viewingScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getDeletedAt() != null) {
            return false;
        }

        Listing listing = listingRepository.findById(schedule.getListingId()).orElse(null);
        if (listing == null || listing.getDeletedAt() != null) {
            return false;
        }

        UUID ownerId = listing.getOwner().getId();
        UUID agentId = listing.getAgent() != null ? listing.getAgent().getId() : null;

        return userId.equals(ownerId) || (agentId != null && userId.equals(agentId));
    }

    /**
     * Returns true if the current user is either:
     * - the client (renter) who created the viewing request, OR
     * - the listing owner / assigned agent (host side).
     *
     * Used to guard GET /api/viewing-schedules/{id} against IDOR and to allow
     * the renter to cancel their own appointment.
     */
    @Transactional(readOnly = true)
    public boolean isParticipant(UUID scheduleId) {
        UUID userId = currentUserId();
        if (userId == null) return false;

        ViewingSchedule schedule = viewingScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getDeletedAt() != null) {
            return false;
        }

        // Client side: the renter who booked the appointment
        if (userId.equals(schedule.getClientId())) {
            return true;
        }

        // Host side: listing owner or agent
        Listing listing = listingRepository.findById(schedule.getListingId()).orElse(null);
        if (listing == null || listing.getDeletedAt() != null) {
            return false;
        }

        UUID ownerId = listing.getOwner().getId();
        UUID agentId = listing.getAgent() != null ? listing.getAgent().getId() : null;

        return userId.equals(ownerId) || (agentId != null && userId.equals(agentId));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the UUID of the authenticated user from the SecurityContext.
     * Returns {@code null} for anonymous users or malformed principals.
     */
    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
