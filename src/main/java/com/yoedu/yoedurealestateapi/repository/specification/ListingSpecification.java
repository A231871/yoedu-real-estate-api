package com.yoedu.yoedurealestateapi.repository.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.yoedu.yoedurealestateapi.domain.entities.Amenity;
import com.yoedu.yoedurealestateapi.domain.entities.Listing;
import com.yoedu.yoedurealestateapi.domain.entities.Province;
import com.yoedu.yoedurealestateapi.domain.entities.Ward;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class ListingSpecification implements Specification<Listing> {

    private final ListingType listingType;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final String title;
    private final String provinceCode;
    private final String wardCode;
    private final Integer minBedrooms;
    private final Integer maxBedrooms;
    private final Integer minBathrooms;
    private final Integer maxBathrooms;
    private final BigDecimal minArea;
    private final BigDecimal maxArea;
    private final List<Integer> amenityIds;

    public ListingSpecification(
        ListingType listingType,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String title,
        String provinceCode,
        String wardCode,
        Integer minBedrooms,
        Integer maxBedrooms,
        Integer minBathrooms,
        Integer maxBathrooms,
        BigDecimal minArea,
        BigDecimal maxArea,
        List<Integer> amenityIds
    ) {
        this.listingType = listingType;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.title = title;
        this.provinceCode = provinceCode;
        this.wardCode = wardCode;
        this.minBedrooms = minBedrooms;
        this.maxBedrooms = maxBedrooms;
        this.minBathrooms = minBathrooms;
        this.maxBathrooms = maxBathrooms;
        this.minArea = minArea;
        this.maxArea = maxArea;
        this.amenityIds = amenityIds;
    }

    @Override
    public Predicate toPredicate(Root<Listing> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        // Filter by listing type (SALE or RENT)
        if (listingType != null) {
            predicates.add(builder.equal(root.get("listingType"), listingType));
        }

        // Filter by bedrooms range
        if (minBedrooms != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("bedrooms"), minBedrooms));
        }
        if (maxBedrooms != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("bedrooms"), maxBedrooms));
        }

        // Filter by bathrooms range
        if (minBathrooms != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("bathrooms"), minBathrooms));
        }
        if (maxBathrooms != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("bathrooms"), maxBathrooms));
        }

        // Filter by area range
        if (minArea != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("area"), minArea));
        }
        if (maxArea != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("area"), maxArea));
        }

        // Filter by title (case-insensitive partial match)
        if (title != null && !title.isBlank()) {
            predicates.add(builder.like(
                builder.lower(root.get("title")),
                "%" + title.toLowerCase() + "%"
            ));
        }

        // Filter by ward
        if (wardCode != null && !wardCode.isBlank()) {
            Join<Listing, Ward> wardJoin = root.join("ward", JoinType.INNER);
            predicates.add(builder.equal(wardJoin.get("code"), wardCode));
        }

        // Filter by province
        if (provinceCode != null && !provinceCode.isBlank()) {
            Join<Listing, Ward> wardJoin = root.join("ward", JoinType.INNER);
            Join<Ward, Province> provinceJoin = wardJoin.join("province", JoinType.INNER);
            predicates.add(builder.equal(provinceJoin.get("code"), provinceCode));
        }

        // Filter by price range
        if (minPrice != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("amountVND"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("amountVND"), maxPrice));
        }

        // Filter by amenities (listing must have all specified amenities)
        if (amenityIds != null && !amenityIds.isEmpty()) {
            Join<Listing, Amenity> amenityJoin = root.join("amenities", JoinType.INNER);
            predicates.add(amenityJoin.get("id").in(amenityIds));

            // Group by listing to ensure listing has all amenities
            query.groupBy(root.get("id"));
            query.having(builder.equal(builder.count(amenityJoin), (long) amenityIds.size()));
        }

        // Exclude soft-deleted listings
        predicates.add(builder.isNull(root.get("deletedAt")));

        return builder.and(predicates.toArray(new Predicate[0]));
    }
}
