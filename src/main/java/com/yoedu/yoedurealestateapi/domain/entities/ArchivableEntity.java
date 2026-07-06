package com.yoedu.yoedurealestateapi.domain.entities;

import jakarta.persistence.Column;
import java.time.LocalDateTime;

public abstract class ArchivableEntity extends AuditableEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
