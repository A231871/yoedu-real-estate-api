package com.yoedu.yoedurealestateapi.repository;

import com.yoedu.yoedurealestateapi.domain.entities.PushToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    Optional<PushToken> findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(UUID userId, String deviceId);

    Optional<PushToken> findFirstByUserIdAndTokenOrderByCreatedAtDesc(UUID userId, String token);

    List<PushToken> findByUserIdAndActiveTrue(UUID userId);

    @Modifying
    @Query("UPDATE PushToken p SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP WHERE p.user.id = :userId AND p.deviceId = :deviceId AND p.active = true")
    int deactivateByUserIdAndDeviceId(@Param("userId") UUID userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("UPDATE PushToken p SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP WHERE p.deviceId = :deviceId AND p.user.id <> :userId AND p.active = true")
    int deactivateOtherUsersByDeviceId(@Param("deviceId") String deviceId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE PushToken p SET p.active = false, p.updatedAt = CURRENT_TIMESTAMP WHERE p.token = :token AND p.user.id <> :userId AND p.active = true")
    int deactivateOtherUsersByToken(@Param("token") String token, @Param("userId") UUID userId);
}
