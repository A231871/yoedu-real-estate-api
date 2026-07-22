package com.yoedu.yoedurealestateapi.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.yoedu.yoedurealestateapi.domain.entities.User;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final AppJwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(AppJwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses claims from a valid, non-expired JWT. Throws JwtException for
     * malformed/invalid/expired tokens. Use {@link #parseClaimsAllowExpired}
     * when you need to inspect an expired token (e.g., refresh token lookup).
     */
    public Claims parseClaims(String token) {
        return parser(token).getPayload();
    }

    /**
     * Parses claims even from an expired JWT (signature must still be valid).
     * Use this only for flows that perform their own expiry check (e.g., the
     * refresh endpoint, which validates expiry via the database row).
     */
    public Claims parseClaimsAllowExpired(String token) {
        try {
            return parser(token).getPayload();
        } catch (ExpiredJwtException ex) {
            return ex.getClaims();
        }
    }

    public String extractUsername(String token) {
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

    /**
     * Returns true if the token is a well-formed, valid refresh token.
     * Returns false (instead of throwing) for malformed, expired, or invalid tokens.
     */
    public boolean isRefreshToken(String token) {
        try {
            return REFRESH_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns true if the token has a valid signature and issuer and is of type refresh,
     * allowing expired tokens (expiration check is handled separately by DB row status).
     */
    public boolean isRefreshTokenAllowExpired(String token) {
        try {
            return REFRESH_TOKEN_TYPE.equals(parseClaimsAllowExpired(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns true if the token is a well-formed, valid access token.
     * Returns false (instead of throwing) for malformed, expired, or invalid tokens.
     */
    public boolean isAccessToken(String token) {
        try {
            return ACCESS_TOKEN_TYPE.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration != null ? expiration.toInstant() : null;
    }

    public String generateJti() {
        return UUID.randomUUID().toString();
    }

    private Jws<Claims> parser(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtlMinutes(), java.time.temporal.ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(generateJti())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .claim("roles", List.of(user.getUserRole().name()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user, String jti) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.refreshTokenTtlDays(), java.time.temporal.ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(jti)
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .signWith(secretKey)
                .compact();
    }
}
