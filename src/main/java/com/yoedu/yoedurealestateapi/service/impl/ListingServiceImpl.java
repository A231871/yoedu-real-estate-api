package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.Utils;
import com.yoedu.yoedurealestateapi.common.exception.NotFoundException;
import com.yoedu.yoedurealestateapi.domain.entities.Amenity;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.ListingMedia;
import com.yoedu.yoedurealestateapi.domain.entities.ListingPrice;
import com.yoedu.yoedurealestateapi.domain.entities.PropertyType;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.entities.Ward;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import com.yoedu.yoedurealestateapi.dto.listing.ListingDetailResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingMediaDto;
import com.yoedu.yoedurealestateapi.dto.listing.ListingSummaryResponse;
import com.yoedu.yoedurealestateapi.dto.listing.ListingUpsertRequest;
import com.yoedu.yoedurealestateapi.repository.AmenityRepository;
import com.yoedu.yoedurealestateapi.repository.ListingRepository;
import com.yoedu.yoedurealestateapi.repository.PropertyTypeRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.repository.WardRepository;
import com.yoedu.yoedurealestateapi.repository.specification.ListingSpecification;
import com.yoedu.yoedurealestateapi.service.ListingService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final WardRepository wardRepository;
    private final AmenityRepository amenityRepository;

    // Utils
    private ListingSummaryResponse toListingSummaryResponse(Listing listing) {
        User agent = listing.getAgent();

        return new ListingSummaryResponse(
            listing.getId().toString(),
            listing.getOwner().getId().toString(),
            agent != null ? agent.getId().toString() : null,
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

    private void apply(ListingUpsertRequest request, Listing newListing) {
        User owner = userRepository
            .findById(UUID.fromString(request.getOwnerId()))
            .orElseThrow(() -> new NotFoundException("Owner not found"));

        Optional<User> agent =
            request.getAgentId() != null
                ? userRepository.findById(UUID.fromString(request.getAgentId()))
                : Optional.empty();

        PropertyType propertyType = propertyTypeRepository
            .findById(Integer.parseInt(request.getPropertyTypeId()))
            .orElseThrow(() ->
                new NotFoundException("Property type not found")
            );

        Ward ward = wardRepository
            .findById(request.getWardCode())
            .orElseThrow(() -> new NotFoundException("Ward not found"));

        Set<Amenity> amenities = new HashSet<>(
            amenityRepository.findAllById(
                request.getAmenityIds().stream().map(Integer::valueOf).toList()
            )
        );

        Set<ListingMedia> listingMedias = request
            .getListingMediaDtos()
            .stream()
            .map(dto -> {
                ListingMedia media = new ListingMedia();
                media.setUrl(dto.url());
                media.setCaption(dto.caption());
                media.setMediaType(dto.mediaType());
                media.setSortOrder(dto.sortOrder());
                media.setPrimary(Boolean.TRUE.equals(dto.isPrimary()));
                media.setListing(newListing);
                return media;
            })
            .collect(Collectors.toSet());

        ListingPrice listingPrice = new ListingPrice();
        listingPrice.setListing(newListing);
        listingPrice.setAmountVND(request.getPrice());

        // Map to Listing entity
        newListing.setOwner(owner);
        newListing.setAgent(agent.orElse(null));
        newListing.setTitle(request.getTitle());
        newListing.setSlug(Utils.generateSlug(request.getTitle()));
        newListing.setDescription(request.getDescription());
        newListing.setAddress(request.getAddress());
        newListing.setArea(request.getArea());
        newListing.setBedrooms(request.getBedrooms());
        newListing.setBathrooms(request.getBathrooms());
        newListing.setFloors(request.getFloors());
        newListing.setListingType(request.getListingType());
        newListing.setPropertyType(propertyType);
        newListing.setWard(ward);
        newListing.setAmenities(amenities.isEmpty() ? null : amenities);
        newListing.setListingMedias(listingMedias);
        newListing.setPrices(new ArrayList<>());
        newListing.getPrices().add(listingPrice);
    }

    // Service methods
    @Override
    public Page<ListingSummaryResponse> getListingSummaries(
        Pageable pageable,
        ListingType listingType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minBedRooms,
        Integer maxBedroom,
        Integer minBathrooms,
        Integer maxBathrooms,
        BigDecimal minArea,
        BigDecimal maxArea,
        String provinceCode,
        String wardCode,
        List<Integer> amenityIds
    ) {
        // Create specification with all filter criteria
        ListingSpecification specification = new ListingSpecification(
            listingType,
            minPrice,
            maxPrice,
            provinceCode,
            wardCode,
            minBedRooms,
            maxBedroom,
            minBathrooms,
            maxBathrooms,
            minArea,
            maxArea,
            amenityIds
        );

        // Fetch listings using the specification and pageable
        Page<Listing> listingPage = listingRepository.findAll(specification, pageable);

        // Map to response DTOs
        return listingPage.map(this::toListingSummaryResponse);
    }

    @Override
    public ListingSummaryResponse toListingSummary(Listing listing) {
        return toListingSummaryResponse(listing);
    }

    @Override
    public Optional<ListingDetailResponse> getListingDetail(String id) {
        UUID uuid = UUID.fromString(id);
        return listingRepository
            .findById(uuid)
            .map(this::toListingDetailResponse);
    }

    @Override
    public void createListing(ListingUpsertRequest request) {
        Listing newListing = new Listing();
        apply(request, newListing);
        listingRepository.save(newListing);
    }

    @Override
    public void updateListing(String id, ListingUpsertRequest request) {
        Listing currentListing = listingRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() ->
                new NotFoundException("Listing with id " + id + " not found")
            );
        apply(request, currentListing);
        listingRepository.save(currentListing);
    }

    @Override
    public void deleteListing(String id) {
        Listing currentListing = listingRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() ->
                new NotFoundException("Listing with id " + id + " not found")
            );
        currentListing.setDeletedAt(Instant.now());
    }
}
