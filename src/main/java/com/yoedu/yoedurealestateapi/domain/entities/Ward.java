package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wards")
public class Ward {

    // Attributes
    @Id
    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "full_name_en")
    private String fullNameEn;

    @Column(name = "code_name")
    private String codeName;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "province_code",
        foreignKey = @ForeignKey(name = "wards_province_code_fkey")
    )
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "administrative_unit_id",
        foreignKey = @ForeignKey(name = "wards_administrative_unit_id_fkey")
    )
    private AdministrativeUnit administrativeUnit;
}
