package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing extends AuditableEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "property_type_id", nullable = false)
    private Integer propertyTypeId;

    @Column(name = "listing_type", nullable = false, length = 50)
    private String listingType;

    @Column(name = "package_id")
    private Integer packageId;

    @Column(name = "priority_level", nullable = false)
    private Short priorityLevel;

    @Column(name = "ward_id", nullable = false)
    private Integer wardId;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "slug", nullable = false, length = 350)
    private String slug;

    @Column(name = "price", nullable = false, precision = 18, scale = 0)
    private BigDecimal price;

    @Column(name = "price_unit", nullable = false, length = 50)
    private String priceUnit;

    @Column(name = "normalized_price_vnd", nullable = false, precision = 18, scale = 0)
    private BigDecimal normalizedPriceVnd;

    @Column(name = "deposit_amount", precision = 18, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "deposit_unit", nullable = false, length = 50)
    private String depositUnit;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "area", precision = 8, scale = 2)
    private BigDecimal area;

    @Column(name = "bedrooms")
    private Short bedrooms;

    @Column(name = "bathrooms")
    private Short bathrooms;

    @Column(name = "floors")
    private Short floors;

    @Column(name = "direction", length = 50)
    private String direction;

    @Column(name = "year_built")
    private Short yearBuilt;

    @Column(name = "interior_status", length = 50)
    private String interiorStatus;

    @Column(name = "legal_status", length = 100)
    private String legalStatus;

    @Column(name = "frontage_width", precision = 8, scale = 2)
    private BigDecimal frontageWidth;

    @Column(name = "road_width", precision = 8, scale = 2)
    private BigDecimal roadWidth;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "suspended_by")
    private UUID suspendedBy;

    @Column(name = "suspend_reason", columnDefinition = "TEXT")
    private String suspendReason;

    @Column(name = "meta_title", length = 300)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Ghi đè phương thức từ BaseEntity để ngăn Hibernate cố gắng ánh xạ
     * thuộc tính boolean "deleted" không có cột tương ứng trong DB của bảng listings.
     */
    @Transient
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}