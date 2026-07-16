package com.yoedu.yoedurealestateapi.security;

import com.yoedu.yoedurealestateapi.domain.entities.Conversation;
import com.yoedu.yoedurealestateapi.repository.ConversationRepository;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import static com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor.USERNAME_ATTRIBUTE;
import static com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor.TOKEN_EXPIRY_ATTRIBUTE;
import static com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor.ROLES_ATTRIBUTE;

/**
 * STOMP-level channel interceptor handling:
 *
 * 1. CONNECT frames:
 *    The JWT has already been validated by WebSocketHandshakeInterceptor at the HTTP layer.
 *    We simply read the pre-populated username from the session attributes and set the
 *    Principal on the STOMP session — no redundant JWT parsing required.
 *
 * 2. SUBSCRIBE frames:
 *    Conversation subscription security: validates that the authenticated user is
 *    either the client or the host of the conversation they are subscribing to.
 *
 * NOTE: In-flight token refresh via @MessageMapping("/auth.refresh") is intentionally
 * NOT implemented. The SimpUserRegistry binds the Principal at CONNECT time and does
 * not update dynamically, so mid-session setUser() on a message-scoped accessor would
 * not persist to the session. Connection-lifetime trust is used instead: the stateful
 * TCP connection remains authenticated until it is closed (by the user, expiry, or
 * by the Redis ban listener).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;
  private final ConversationRepository conversationRepository;
  private final org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();
  private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

  @org.springframework.beans.factory.annotation.Autowired
  public void setMessagingTemplate(@org.springframework.context.annotation.Lazy org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      handleConnectFrame(accessor);
    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      try {
        checkTokenExpiry(accessor);
        authorizeSubscribeFrame(accessor);
      } catch (AccessDeniedException | IllegalArgumentException ex) {
        log.warn("STOMP SUBSCRIBE blocked: {}", ex.getMessage());
        Principal p = accessor.getUser();
        if (p != null) {
          try {
            messagingTemplate.convertAndSendToUser(
                p.getName(), "/queue/errors",
                java.util.Map.of("error", ex.getMessage()));
          } catch (Exception sendEx) {
            log.error("Failed to send private error message: {}", sendEx.getMessage());
          }
        }
        return null; // Drop frame silently — keep TCP connection alive
      }
    } else if (StompCommand.SEND.equals(accessor.getCommand())) {
      checkTokenExpiry(accessor);
    }

    return message;
  }

  private void checkTokenExpiry(StompHeaderAccessor accessor) {
    java.time.Instant expiry = (java.time.Instant) accessor.getSessionAttributes().get(TOKEN_EXPIRY_ATTRIBUTE);
    if (expiry != null && java.time.Instant.now().isAfter(expiry)) {
      log.warn("STOMP SEND rejected: token expired for session {}", accessor.getSessionId());
      throw new AccessDeniedException("Your session token has expired. Please reconnect.");
    }
  }

  // ── CONNECT ────────────────────────────────────────────────────────────────

  /**
   * Reads the username pre-validated by WebSocketHandshakeInterceptor from the HTTP
   * session attributes and binds it as the STOMP session Principal.
   *
   * The STOMP Authorization header is intentionally ignored here — the HTTP handshake
   * is the single source of truth for authentication.
   */
  private void handleConnectFrame(StompHeaderAccessor accessor) {
    String username = (String) accessor.getSessionAttributes().get(USERNAME_ATTRIBUTE);

    if (username == null || username.isBlank()) {
      // Should never happen if HandshakeInterceptor is correctly wired,
      // but reject defensively in case of misconfiguration.
      log.error("STOMP CONNECT: session attributes missing 'username' — possible HandshakeInterceptor misconfiguration");
      throw new IllegalStateException("WebSocket session is not authenticated");
    }

    // Reconstruct roles from pre-validated session attributes.
    List<SimpleGrantedAuthority> authorities;
    try {
      @SuppressWarnings("unchecked")
      List<String> roles = (List<String>) accessor.getSessionAttributes().get(ROLES_ATTRIBUTE);
      if (roles != null) {
        authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
      } else {
        authorities = List.of();
      }
    } catch (Exception ex) {
      authorities = List.of();
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(username, null, authorities);

    // accessor.setUser() persists the Principal across all frames in this STOMP session.
    // This is what SimpUserRegistry uses to route /user/** messages.
    accessor.setUser(authentication);

    log.debug("STOMP CONNECT: Principal set for user '{}'", username);
  }

  // ── SUBSCRIBE ──────────────────────────────────────────────────────────────

  private static final String CONV_PATTERN = "/topic/conversation/{conversationId}/**";

  /**
   * Prevents users from subscribing to conversation topics they are not a participant of.
   * Applies to destinations matching: /topic/conversation/{conversationId}/**
   */
  private void authorizeSubscribeFrame(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || !pathMatcher.match(CONV_PATTERN, destination)) {
      return;
    }

    UUID conversationId;
    try {
      java.util.Map<String, String> vars = pathMatcher.extractUriTemplateVariables(CONV_PATTERN, destination);
      String convIdStr = vars.get("conversationId");
      conversationId = UUID.fromString(convIdStr);
    } catch (Exception e) {
      log.warn("STOMP SUBSCRIBE: invalid conversation UUID in destination '{}'", destination);
      throw new IllegalArgumentException("Invalid conversation UUID in subscription destination");
    }

    Principal principal = accessor.getUser();
    if (principal == null) {
      log.warn("STOMP SUBSCRIBE: no Principal on session (destination: {})", destination);
      throw new AccessDeniedException("User is not authenticated");
    }

    UUID userId;
    try {
      userId = UUID.fromString(principal.getName());
    } catch (IllegalArgumentException e) {
      log.warn("STOMP SUBSCRIBE: Principal name is not a valid UUID: '{}'", principal.getName());
      throw new AccessDeniedException("Invalid user identifier");
    }

    if (!conversationRepository.existsByIdAndParticipant(conversationId, userId)) {
      log.warn("STOMP SUBSCRIBE: Access denied — user {} is not a participant of conversation {}",
          userId, conversationId);
      throw new AccessDeniedException("You are not a participant in this conversation");
    }

    log.debug("STOMP SUBSCRIBE: user {} authorized for conversation {}", userId, conversationId);
  }
}
