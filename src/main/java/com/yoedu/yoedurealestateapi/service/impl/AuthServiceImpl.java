package com.yoedu.yoedurealestateapi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoedu.yoedurealestateapi.common.exception.BadRequestException;
import com.yoedu.yoedurealestateapi.config.AppFrontendConfig;
import com.yoedu.yoedurealestateapi.domain.entities.RefreshToken;
import com.yoedu.yoedurealestateapi.domain.entities.User;
import com.yoedu.yoedurealestateapi.domain.entities.UserProfile;
import com.yoedu.yoedurealestateapi.domain.enums.AuthProvider;
import com.yoedu.yoedurealestateapi.domain.enums.UserRole;
import com.yoedu.yoedurealestateapi.domain.enums.UserStatus;
import com.yoedu.yoedurealestateapi.dto.auth.*;
import com.yoedu.yoedurealestateapi.redis.entity.PendingUser;
import com.yoedu.yoedurealestateapi.repository.RefreshTokenRepository;
import com.yoedu.yoedurealestateapi.repository.UserRepository;
import com.yoedu.yoedurealestateapi.security.AppJwtProperties;
import com.yoedu.yoedurealestateapi.security.JwtService;
import com.yoedu.yoedurealestateapi.security.TokenType;
import com.yoedu.yoedurealestateapi.service.AuthService;
import com.yoedu.yoedurealestateapi.service.EmailService;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> pendingUserRedisTemplate;
    private final AppJwtProperties jwtProperties;
    private final AppFrontendConfig frontendConfig;
    private final ObjectMapper objectMapper;


    @Override
    public void register(RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email()).isPresent()) {
            throw new BadRequestException("This email has already been taken");
        }

        // Create pending user and save to Redis cache
        UUID pendingUserId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.password());

        PendingUser pendingUser = new PendingUser(
            pendingUserId,
            request.email(),
            passwordHash,
            request.fullName(),
            request.phone()
        );

        Duration ttl = Duration.ofMinutes(jwtProperties.verificationTokenTtlMinutes());
        pendingUserRedisTemplate.opsForValue().set(pendingUserKey(pendingUserId), pendingUser, ttl);

        String verificationToken = jwtService.generateVerificationToken(pendingUserId.toString(), request.email());
        // TODO: This is development URL
        String verificationUrl = frontendConfig.url() + "/auth/verify?token=" + verificationToken;
        emailService.sendEmail(
            request.email(),
            "Xác minh tài khoản",
            "Liên kết xác minh của bạn: " + verificationUrl
        );
    }

    @Override
    @Transactional
    public Pair<AuthResponse, String> verifyRegistration(String token) {
        // Validate verification token
        if (!jwtService.validateToken(token, TokenType.VERIFICATION_TOKEN)) {
            throw new BadRequestException("Verification link is invalid or has expired");
        }

        // Extract pending user from Redis cache
        UUID pendingUserId;
        try {
            pendingUserId = UUID.fromString(jwtService.extractUserId(token));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Verification link is invalid or has expired");
        }

        String key = pendingUserKey(pendingUserId);
        Object rawValue = pendingUserRedisTemplate.opsForValue().get(key);
        PendingUser pendingUser = (rawValue != null)
            ? objectMapper.convertValue(rawValue, PendingUser.class)
            : null;
        if (pendingUser == null) {
            throw new BadCredentialsException("Verification link is invalid or has expired");
        }

        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(pendingUser.getEmail()).isPresent()) {
            pendingUserRedisTemplate.delete(key);
            throw new BadRequestException("This email has already been taken");
        }

        // Create actual user and save into database
        User user = new User();
        user.setEmail(pendingUser.getEmail());
        user.setPasswordHash(pendingUser.getPasswordHash());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setUserRole(UserRole.REGULAR_USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFullName(pendingUser.getFullName());
        profile.setPhone(pendingUser.getPhone());
        user.setProfile(profile);

        userRepository.save(user);
        pendingUserRedisTemplate.delete(key);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return Pair.of(
            new AuthResponse(accessToken, user.getId().toString(), user.getEmail()),
            refreshToken
        );
    }

    @Override
    public Pair<AuthResponse, String> login(LoginRequest request) {
        // Validate user credentials
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // Generate new token pairs
        Pair<String, String> tokens = createTokenPairs(user);

        return Pair.of(
            new AuthResponse(tokens.getLeft(), user.getId().toString(), user.getEmail()),
            tokens.getRight()
        );
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        String tokenHash = jwtService.hashToken(token);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

    @Override
    @Transactional
    public Pair<AuthResponse, String> refresh(RefreshRequest request) {
        // Validate received token
        String receivedRefreshTokenString = request.refreshToken();
        if (!jwtService.validateToken(receivedRefreshTokenString, TokenType.REFRESH_TOKEN)) {
            throw new BadRequestException("Invalid refresh token");
        }

        // Verify token by looking up token hash
        RefreshToken storedRefreshToken = refreshTokenRepository
            .findByTokenHash(jwtService.hashToken(receivedRefreshTokenString))
            .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (storedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        // Generate new token pair
        User user = userRepository
            .findById(UUID.fromString(jwtService.extractUserId(receivedRefreshTokenString)))
            .orElseThrow(() -> new BadRequestException("User not found"));

        Pair<String, String> tokens = createTokenPairs(user);

        return Pair.of(
            new AuthResponse(tokens.getLeft(), user.getId().toString(), user.getEmail()),
            tokens.getRight()
        );
    }

    /* Helper methods */

    private Pair<String, String> createTokenPairs(User user) {
        // Generate a JWT pair (access and refresh)
        String newAccessTokenString = jwtService.generateAccessToken(user);
        String newRefreshTokenString = jwtService.generateRefreshToken(user);

        // Save refresh token hash in database
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(jwtService.hashToken(newRefreshTokenString));
        newRefreshToken.setExpiresAt(
            jwtService.extractExpiration(newRefreshTokenString)
        );
        refreshTokenRepository.save(newRefreshToken);

        return Pair.of(newAccessTokenString, newRefreshTokenString);
    }

    private String pendingUserKey(UUID pendingUserId) {
        return "auth:pending-user:" + pendingUserId;
    }

}
