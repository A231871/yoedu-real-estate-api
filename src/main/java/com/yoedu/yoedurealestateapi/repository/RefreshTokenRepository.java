package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all non-revoked refresh tokens for a user.
     * Called before issuing a new token to prevent unlimited session accumulation.
     * clearAutomatically = true ensures Hibernate L1 cache is invalidated after the native UPDATE.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE refresh_tokens SET revoked = TRUE, revoked_at = TIMESTAMP '1970-01-01 00:00:00' WHERE user_id = :userId AND revoked = FALSE", nativeQuery = true)
    int revokeAllUserTokens(@Param("userId") UUID userId);

    /**
     * Revokes a single refresh token by its hash.
     * Used during token rotation to invalidate the consumed token.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE refresh_tokens SET revoked = TRUE, revoked_at = now() WHERE token_hash = :tokenHash", nativeQuery = true)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Deletes expired and revoked tokens older than 7 days.
     * Called periodically by RefreshTokenCleanupCronJob to prevent table bloat.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM refresh_tokens WHERE (expires_at < now() OR revoked = TRUE) AND created_at < now() - INTERVAL '7 days'", nativeQuery = true)
    int deleteExpiredAndRevoked();
}
