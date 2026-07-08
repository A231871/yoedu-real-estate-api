package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
public class User extends AuditableEntity {

  public enum Status {
    ACTIVE, SUSPENDED, PENDING_VERIFY
  }

  @Column(name = "email", nullable = false)
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "phone")
  private String phone;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", nullable = false)
  private AuthProvider authProvider = AuthProvider.LOCAL;

  @Column(name = "provider_id")
  private String providerId;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "roles", nullable = false)
  private List<String> roles;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.PENDING_VERIFY;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "bio", columnDefinition = "TEXT")
  private String bio;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Version
  @Column(name = "version", nullable = false)
  private Integer version = 0;
}
