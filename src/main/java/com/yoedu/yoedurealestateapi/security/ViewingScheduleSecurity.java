package com.yoedu.yoedurealestateapi.security;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return false;
        }

        ViewingSchedule schedule = viewingScheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null || schedule.getDeletedAt() != null) {
            return false;
        }

        Listing listing = listingRepository.findById(schedule.getListingId()).orElse(null);
        if (listing == null || listing.getDeletedAt() != null) {
            return false;
        }

        UUID ownerId = listing.getOwnerId();
        UUID agentId = listing.getAgentId();

        return userId.equals(ownerId) || (agentId != null && userId.equals(agentId));
    }
}
