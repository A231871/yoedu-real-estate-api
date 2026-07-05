package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 50)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(
        name = "roles",
        nullable = false,
        columnDefinition = "character varying(50)[]"
    )
    private String[] roles = new String[] { "RENTER" };

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_VERIFY";

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
