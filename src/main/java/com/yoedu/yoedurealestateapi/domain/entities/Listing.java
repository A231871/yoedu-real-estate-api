package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.enums.ListingStatus;
import com.yoedu.yoedurealestateapi.domain.enums.ListingType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "listings")
@Entity
public class Listing extends ArchivableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "area", nullable = false, precision = 8, scale = 2)
    private BigDecimal area;

    @Column(name = "bedrooms")
    private Integer bedrooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "floors")
    private Integer floors;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ListingStatus status = ListingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code", nullable = false)
    private Ward ward;

    @ManyToMany
    @JoinTable(
        name = "listing_amenities",
        joinColumns = { @JoinColumn(name = "listing_id") },
        inverseJoinColumns = { @JoinColumn(name = "amenity_id") }
    )
    Set<Amenity> amenities = new HashSet<>();

    @OneToMany(
        mappedBy = "listing",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<ListingMedia> listingMedias = new HashSet<>();

    @OneToMany(
        mappedBy = "listing",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @OrderBy("createdAt DESC")
    private List<ListingPrice> prices = new ArrayList<>();
}
