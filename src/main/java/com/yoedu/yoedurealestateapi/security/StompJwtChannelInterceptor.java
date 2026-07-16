package com.yoedu.yoedurealestateapi.security;

import com.yoedu.yoedurealestateapi.domain.entities.Conversation;
import com.yoedu.yoedurealestateapi.repository.ConversationRepository;
import io.jsonwebtoken.JwtException;
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

/**
 * Intercepts inbound STOMP frames to:
 * 1. Authenticate the user from the JWT token provided in the CONNECT frame's "Authorization" native header.
 * 2. Enforce subscription-level security to prevent users from subscribing to conversations they don't participate in.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";
  private final JwtService jwtService;
  private final ConversationRepository conversationRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      authenticateConnectFrame(accessor);
    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      authorizeSubscribeFrame(accessor);
    }

    return message;
  }

  private void authenticateConnectFrame(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      log.warn("STOMP CONNECT frame missing Authorization header — rejecting");
      throw new IllegalArgumentException("Missing Authorization header in STOMP CONNECT frame");
    }

    String token = authHeader.substring(BEARER_PREFIX.length());
    try {
      if (!jwtService.isAccessToken(token)) {
        throw new JwtException("Not an access token");
      }

      String username = jwtService.extractUsername(token);
      List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
          .toList();

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(username, null, authorities);

      // Attach the authentication to the STOMP session so @AuthenticationPrincipal works
      accessor.setUser(authentication);

      log.debug("STOMP CONNECT authenticated for user: {}", username);
    } catch (JwtException | IllegalArgumentException ex) {
      log.warn("STOMP CONNECT JWT validation failed: {}", ex.getMessage());
      throw new IllegalArgumentException("Invalid or expired JWT token in STOMP CONNECT frame");
    }
  }

  private void authorizeSubscribeFrame(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith("/topic/conversation/")) {
      return;
    }

    // Extract the conversation ID from destination (e.g. "/topic/conversation/{id}")
    String convIdStr = destination.substring("/topic/conversation/".length());
    UUID conversationId;
    try {
      conversationId = UUID.fromString(convIdStr);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid conversation UUID in subscription destination: {}", convIdStr);
      throw new IllegalArgumentException("Invalid conversation UUID");
    }

    Principal principal = accessor.getUser();
    if (principal == null) {
      log.warn("Unauthorized subscription attempt to destination {} (no user principal found)", destination);
      throw new AccessDeniedException("User is not authenticated");
    }

    String username = principal.getName();
    UUID userId;
    try {
      userId = UUID.fromString(username);
    } catch (IllegalArgumentException e) {
      log.warn("Username in principal is not a valid UUID: {}", username);
      throw new AccessDeniedException("User ID is invalid");
    }

    Conversation conversation = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

    boolean isClient = conversation.getClient().getId().equals(userId);
    boolean isHost = conversation.getHost().getId().equals(userId);

    if (!isClient && !isHost) {
      log.warn("Access Denied: User {} tried to subscribe to conversation {} without participating", userId, conversationId);
      throw new AccessDeniedException("You are not a participant in this conversation");
    }

    log.debug("User {} authorized to subscribe to conversation {}", userId, conversationId);
  }
}
