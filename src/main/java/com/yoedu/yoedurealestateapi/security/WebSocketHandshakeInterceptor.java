package com.yoedu.yoedurealestateapi.security;

import com.yoedu.yoedurealestateapi.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * HTTP-level Layer 7 DoS prevention interceptor.
 *
 * Validates the JWT token BEFORE the WebSocket TCP upgrade is allowed to complete.
 * This stops unauthenticated or banned users at the HTTP handshake phase,
 * preventing the server from wasting resources on the TCP upgrade.
 *
 * Protocol convention (browsers cannot set custom HTTP headers on WebSocket):
 *   The client includes the JWT in the Sec-WebSocket-Protocol header alongside
 *   the STOMP sub-protocol, e.g.:
 *     Sec-WebSocket-Protocol: v11.stomp, Bearer.<jwt-token>
 *
 * This interceptor:
 *  1. Parses all sub-protocol values (handles comma-separated strings from browsers).
 *  2. Extracts and validates the Bearer token.
 *  3. Checks the Redis ban blacklist (key: "banned:user:{userId}").
 *  4. On success: stores username in session attributes and negotiates the STOMP
 *     sub-protocol (NOT the Bearer token) in the response header.
 *  5. On failure: returns HTTP 401/403 and false, preventing TCP upgrade.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

  private static final String BEARER_PROTOCOL_PREFIX = "Bearer.";
  private static final List<String> SUPPORTED_STOMP_PROTOCOLS =
      List.of("v12.stomp", "v11.stomp", "v10.stomp");
  public static final String USERNAME_ATTRIBUTE = "username";
  public static final String TOKEN_EXPIRY_ATTRIBUTE = "tokenExpiry";
  public static final String ROLES_ATTRIBUTE = "roles";
  public static final String BAN_KEY_PREFIX = "banned:user:";

  private final JwtService jwtService;
  private final RedisTemplate<String, String> redisTemplate;

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    // ── Step 1: Parse Sec-WebSocket-Protocol header ────────────────────────
    // Browsers may send multiple header values OR a single comma-separated string.
    // We join everything and split by comma to handle both cases.
    List<String> rawProtocols = request.getHeaders().get("Sec-WebSocket-Protocol");
    if (rawProtocols == null || rawProtocols.isEmpty()) {
      log.warn("WS handshake rejected: missing Sec-WebSocket-Protocol header");
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    String[] parts = String.join(",", rawProtocols).split(",");

    String bearerToken = null;
    String negotiatedStompProtocol = null;

    for (String part : parts) {
      String trimmed = part.trim();
      if (trimmed.startsWith(BEARER_PROTOCOL_PREFIX)) {
        bearerToken = trimmed.substring(BEARER_PROTOCOL_PREFIX.length());
      } else if (SUPPORTED_STOMP_PROTOCOLS.contains(trimmed) && negotiatedStompProtocol == null) {
        // Pick the first supported STOMP protocol the client offered
        negotiatedStompProtocol = trimmed;
      }
    }

    if (bearerToken == null || bearerToken.isBlank()) {
      log.warn("WS handshake rejected: no Bearer token found in Sec-WebSocket-Protocol");
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    // ── Step 2: Validate JWT ───────────────────────────────────────────────
    String username;
    try {
      if (!jwtService.isAccessToken(bearerToken)) {
        log.warn("WS handshake rejected: token is not an access token");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
      }
      username = jwtService.extractUsername(bearerToken);
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn("WS handshake rejected: invalid or expired JWT — {}", ex.getMessage());
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      return false;
    }

    // ── Step 3: Check Redis ban blacklist ──────────────────────────────────
    // Key set by the admin/ban service when a user is suspended.
    // Even if the JWT is still valid, a banned user cannot reconnect.
    Boolean isBanned = redisTemplate.hasKey(BAN_KEY_PREFIX + username);
    if (Boolean.TRUE.equals(isBanned)) {
      log.warn("WS handshake rejected: user {} is banned", username);
      response.setStatusCode(HttpStatus.FORBIDDEN);
      return false;
    }

    // ── Step 4: Store username, expiry, and roles in session attributes ──────
    // The StompJwtChannelInterceptor reads this on the CONNECT frame
    // to set the Principal — avoiding a duplicate JWT parse.
    attributes.put(USERNAME_ATTRIBUTE, username);
    try {
      java.time.Instant expiry = jwtService.extractExpiration(bearerToken);
      attributes.put(TOKEN_EXPIRY_ATTRIBUTE, expiry);
      List<String> roles = jwtService.extractRoles(bearerToken);
      attributes.put(ROLES_ATTRIBUTE, roles);
    } catch (Exception ex) {
      log.warn("Failed to extract expiry or roles from token: {}", ex.getMessage());
    }

    // ── Step 5: Negotiate the STOMP sub-protocol (NOT the Bearer token) ────
    // This is critical: echoing back "Bearer.<token>" would cause the browser
    // to immediately drop the connection with a sub-protocol mismatch error.
    // Per RFC 6455, if the client requests sub-protocols, the server MUST
    // return exactly one. Approving with null protocol causes silent browser disconnect.
    if (negotiatedStompProtocol == null) {
      log.warn("WS handshake rejected: no supported STOMP sub-protocol found in Sec-WebSocket-Protocol header");
      response.setStatusCode(HttpStatus.BAD_REQUEST);
      return false;
    }
    response.getHeaders().set("Sec-WebSocket-Protocol", negotiatedStompProtocol);

    log.debug("WS handshake approved for user: {} (protocol: {})", username, negotiatedStompProtocol);
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // No-op — cleanup handled by WebSocketHandlerDecorator in WebSocketConfig
  }
}
