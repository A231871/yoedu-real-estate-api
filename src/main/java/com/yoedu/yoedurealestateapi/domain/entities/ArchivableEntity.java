package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ArchivableEntity extends AuditableEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

}
