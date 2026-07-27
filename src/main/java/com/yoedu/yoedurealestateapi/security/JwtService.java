package com.yoedu.yoedurealestateapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import com.yoedu.yoedurealestateapi.domain.entities.User;

@Service
public class JwtService {

    private final String TOKEN_TYPE_CLAIM = "token_type";
    private final AppJwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(AppJwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /* JWT parser */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        Object rolesObject = claims.get("roles");
        if (rolesObject instanceof List<?> rolesList) {
            return rolesList.stream().map(String::valueOf).toList();
        }
        return Collections.emptyList();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration != null ? expiration.toInstant() : null;
    }

    /* Token validator */
    public boolean validateToken(String token, TokenType tokenType) {
        try {
            return tokenType.name().equals(
                parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class)
            );
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /* Token hasher */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /* JWT Token Generators */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(generateJti())
                .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS_TOKEN.name())
                .claim("roles", List.of(user.getUserRole().name()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(generateJti())
                .claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH_TOKEN.name())
                .signWith(secretKey)
                .compact();
    }

    public String generateVerificationToken(String userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.verificationTokenTtlMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim(TOKEN_TYPE_CLAIM, TokenType.VERIFICATION_TOKEN.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(generateJti())
                .signWith(secretKey)
                .compact();
    }

    /* Helper methods */
    private String generateJti() {
        return UUID.randomUUID().toString();
    }
}
