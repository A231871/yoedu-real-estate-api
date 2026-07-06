package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "administrative_units")
public class AdministrativeUnit {

    // Attributes
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "full_name_en")
    private String fullNameEn;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "short_name_en")
    private String shortNameEn;

    @Column(name = "code_name")
    private String codeName;

    @Column(name = "code_name_en")
    private String codeNameEn;

    // Relationships
    @OneToMany(mappedBy = "administrativeUnit")
    private Set<Province> provinces = new HashSet<>();

    @OneToMany(mappedBy = "administrativeUnit")
    private Set<Ward> wards = new HashSet<>();
}
