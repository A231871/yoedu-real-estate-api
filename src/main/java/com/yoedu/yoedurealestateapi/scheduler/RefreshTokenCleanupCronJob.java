package com.yoedu.yoedurealestateapi.scheduler;

import com.yoedu.yoedurealestateapi.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically purges expired and revoked refresh tokens from the database.
 *
 * <p>Without this job, every login accumulates a new row in {@code refresh_tokens},
 * causing the table to bloat indefinitely and degrading query performance.
 * Only tokens older than 7 days are deleted, preserving recent revoked tokens
 * for a short audit window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupCronJob {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.auth.cleanup-cron:0 0 3 * * *}") // Daily at 03:00 by default
    @SchedulerLock(
            name = "refreshTokenCleanup",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT10M")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        int deleted = refreshTokenRepository.deleteExpiredAndRevoked();
        if (deleted > 0) {
            log.info("Purged {} expired/revoked refresh token(s) from the database", deleted);
        }
    }
}
