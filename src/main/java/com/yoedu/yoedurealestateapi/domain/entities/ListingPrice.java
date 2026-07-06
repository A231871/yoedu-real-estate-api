package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "listing_prices")
public class ListingPrice extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "amount_vnd", nullable = false, precision = 20, scale = 2)
    private BigDecimal amountVND;
}
