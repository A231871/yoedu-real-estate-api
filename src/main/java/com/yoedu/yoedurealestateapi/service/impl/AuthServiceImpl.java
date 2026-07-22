package com.yoedu.yoedurealestateapi.service.impl;

import com.yoedu.yoedurealestateapi.common.exception.ConflictException;
import com.yoedu.yoedurealestateapi.common.exception.UserBannedException;
import com.yoedu.yoedurealestateapi.domain.entities.RefreshToken;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import com.yoedu.yoedurealestateapi.domain.enums.UserRole;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;
import com.yoedu.yoedurealestateapi.dto.auth.LoginRequest;
import com.yoedu.yoedurealestateapi.dto.auth.LoginResponse;
import com.yoedu.yoedurealestateapi.dto.auth.RefreshRequest;
import com.yoedu.yoedurealestateapi.dto.auth.RegisterRequest;
import com.yoedu.yoedurealestateapi.dto.user.UserProfileResponse;
import com.yoedu.yoedurealestateapi.mapper.UserProfileMapper;
import com.yoedu.yoedurealestateapi.repository.RefreshTokenRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.security.JwtService;
import com.yoedu.yoedurealestateapi.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_DEVICE_INFO_LENGTH = 497;
    private static final long REFRESH_GRACE_PERIOD_SECONDS = 30;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileMapper userProfileMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String normalizedEmail = request.email() != null
                ? request.email().trim().toLowerCase(java.util.Locale.ROOT)
                : "";

        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail).isPresent()) {
            throw new ConflictException("Email này đã được sử dụng. Vui lòng chọn email khác.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() != null ? request.fullName().trim() : "");
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setUserRole(UserRole.GUEST);
        user.setStatus(UserStatus.PENDING_VERIFY);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        return userProfileMapper.toResponse(savedUser);
    }

    /**
     * No @Transactional on login(): BCrypt verification is CPU-intensive (~100–300ms).
     * Holding a DB connection during that time would exhaust the connection pool.
     * The DB modifications inside login() use TransactionTemplate to ensure
     * @Modifying queries execute inside a valid transaction boundary without
     * holding a connection during BCrypt matching.
     */
    @Override
    public LoginResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        String normalizedEmail = request.email() != null
                ? request.email().trim().toLowerCase(java.util.Locale.ROOT)
                : "";

        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Username or password is invalid"));

        // BCrypt check runs outside any transaction — no DB connection held here
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Username or password is invalid");
        }

        validateUserStatus(user);

        String accessToken = jwtService.generateAccessToken(user);
        String jti = jwtService.generateJti();
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        Instant expiresAt = jwtService.extractExpiration(refreshToken);
        String tokenHash = hashToken(refreshToken);
        String safeDeviceInfo = truncateDeviceInfo(deviceInfo);

        // Execute DB updates inside an explicit transaction block to prevent TransactionRequiredException
        transactionTemplate.executeWithoutResult(status -> {
            refreshTokenRepository.revokeAllUserTokens(user.getId());
            refreshTokenRepository.insertRefreshToken(user.getId(), tokenHash, safeDeviceInfo, ipAddress, expiresAt);
        });

        return new LoginResponse(accessToken, refreshToken, userProfileMapper.toResponse(user));
    }

    /**
     * Rotating refresh token strategy with Breach Detection & Concurrency Grace Period:
     * 1. Validate the provided refresh token signature and claims via JwtService.
     * 2. Validate against the database.
     * 3. If token is revoked:
     *    - Within 30s grace period: allow request (handles SPA parallel request race conditions).
     *    - After 30s grace period: SUSPECTED BREACH! Revoke all tokens for this user immediately.
     * 4. Revoke consumed token and issue new rotating token pair.
     */
    @Override
    @Transactional
    public LoginResponse refresh(RefreshRequest request, String ipAddress, String deviceInfo) {
        if (!jwtService.isRefreshTokenAllowExpired(request.refreshToken())) {
            throw new BadCredentialsException("Refresh token is invalid or has already been used");
        }

        String tokenHash = hashToken(request.refreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token is invalid or has already been used"));

        if (storedToken.isRevoked()) {
            Instant graceLimit = storedToken.getRevokedAt() != null
                    ? storedToken.getRevokedAt().plusSeconds(REFRESH_GRACE_PERIOD_SECONDS)
                    : Instant.EPOCH;

            if (Instant.now().isAfter(graceLimit)) {
                // Potential token theft: Revoke ALL tokens for this user family immediately!
                refreshTokenRepository.revokeAllUserTokens(storedToken.getUser().getId());
                throw new BadCredentialsException("Compromised session detected. All sessions have been logged out for security.");
            }
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token has expired. Please log in again.");
        }

        User user = storedToken.getUser();
        validateUserStatus(user);

        // Revoke consumed token
        refreshTokenRepository.revokeByTokenHash(tokenHash);

        // Generate new token pair
        String newAccessToken = jwtService.generateAccessToken(user);
        String newJti = jwtService.generateJti();
        String newRefreshToken = jwtService.generateRefreshToken(user, newJti);

        Instant expiresAt = jwtService.extractExpiration(newRefreshToken);
        String newTokenHash = hashToken(newRefreshToken);
        String safeDeviceInfo = truncateDeviceInfo(deviceInfo);

        refreshTokenRepository.insertRefreshToken(user.getId(), newTokenHash, safeDeviceInfo, ipAddress, expiresAt);

        return new LoginResponse(newAccessToken, newRefreshToken, userProfileMapper.toResponse(user));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void validateUserStatus(User user) {
        if (user.getDeletedAt() != null) {
            throw new BadCredentialsException("Tài khoản của bạn không tồn tại hoặc đã bị xóa.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UserBannedException("Tài khoản của bạn đã bị khóa.");
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFY) {
            throw new UserBannedException("Tài khoản của bạn chưa được xác minh email. Vui lòng kiểm tra hộp thư của bạn.");
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute SHA-256 token hash", e);
        }
    }

    private String truncateDeviceInfo(String deviceInfo) {
        if (deviceInfo == null) return "Unknown";
        if (deviceInfo.length() <= 500) return deviceInfo;
        return deviceInfo.substring(0, MAX_DEVICE_INFO_LENGTH) + "...";
    }
}
