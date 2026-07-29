package com.yoedu.yoedurealestateapi.domain.entities;

import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import com.yoedu.yoedurealestateapi.domain.enums.UserRole;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Getter
@Setter
@Entity
@Table(name = "users")
@Audited
public class User extends ArchivableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotAudited
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

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole = UserRole.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFY;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;
}
