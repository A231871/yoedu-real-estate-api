package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.domain.entities.Amenity;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ListingMedia;
import com.yoedu.yoedurealestateapi.domain.entities.Ward;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingMediaDto;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.service.ListingService;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;

    // Utils
    private ListingSummaryResponse toListingSummaryResponse(Listing listing) {
        return new ListingSummaryResponse(
            listing.getId().toString(),
            listing.getOwner().getId().toString(),
            listing.getAgent().getId().toString(),
            listing.getTitle(),
            listing.getSlug(),
            listing.getDescription(),
            listing.getWard().getProvince().getName(),
            listing
                .getListingMedias()
                .stream()
                .sorted(
                    Comparator.comparing(ListingMedia::isPrimary)
                        .reversed()
                        .thenComparing(ListingMedia::getSortOrder)
                )
                .limit(3)
                .map(this::toListingMediaDto)
                .toList(),
            listing.getArea(),
            listing.getListingType(),
            listing.getPrices().isEmpty()
                ? null
                : listing.getPrices().getFirst().getAmountVND()
        );
    }

    private ListingDetailResponse toListingDetailResponse(Listing listing) {
        Ward ward = listing.getWard();

        return new ListingDetailResponse(
            listing.getId().toString(),
            listing.getOwner().getId().toString(),
            listing.getAgent() != null
                ? listing.getAgent().getId().toString()
                : null,

            listing.getTitle(),
            listing.getSlug(),
            listing.getDescription(),
            listing.getAddress(),

            listing.getArea(),
            listing.getBedrooms(),
            listing.getBathrooms(),
            listing.getFloors(),

            listing.getStatus(),
            listing.getListingType(),
            listing.getPropertyType().getName(),
            ward.getProvince().getName(),
            ward.getName(),

            listing.getPrices().isEmpty()
                ? null
                : listing.getPrices().getFirst().getAmountVND(),

            listing
                .getListingMedias()
                .stream()
                .sorted(
                    Comparator.comparing(ListingMedia::isPrimary)
                        .reversed()
                        .thenComparing(ListingMedia::getSortOrder)
                )
                .map(this::toListingMediaDto)
                .toList(),

            listing
                .getAmenities()
                .stream()
                .sorted(Comparator.comparing(Amenity::getSortOrder))
                .map(Amenity::getName)
                .toList()
        );
    }

    private ListingMediaDto toListingMediaDto(ListingMedia media) {
        return new ListingMediaDto(
            media.getUrl(),
            media.getCaption(),
            media.getMediaType(),
            media.getSortOrder(),
            media.isPrimary()
        );
    }

    private void apply(ListingUpsertRequest request, Listing newListing) {}

    // Service methods
    @Override
    public List<ListingSummaryResponse> getListingSummaries() {
        return listingRepository
            .findAll()
            .stream()
            .map(this::toListingSummaryResponse)
            .toList();
    }

    @Override
    public Optional<ListingDetailResponse> getListingDetail(Long id) {
        return listingRepository
            .findById(id)
            .map(this::toListingDetailResponse);
    }

    @Override
    public void createListing(ListingUpsertRequest request) {
        Listing newListing = new Listing();
        apply(request, newListing);
    }
}
