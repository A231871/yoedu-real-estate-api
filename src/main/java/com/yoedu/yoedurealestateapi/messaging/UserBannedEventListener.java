package com.yoedu.yoedurealestateapi.messaging;

import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

/**
 * Redis Pub/Sub listener for real-time user ban enforcement.
 *
 * When the admin service bans a user it publishes to the Redis channel "user:banned"
 * with the banned user's UUID as the message body. This listener receives the event
 * and immediately forcefully closes ALL of that user's active WebSocket sessions
 * (including multiple browser tabs).
 *
 * CloseStatus 4403 is a custom application-level code meaning "Forbidden — Account Suspended".
 * The frontend should listen for this code and redirect to a "your account has been suspended" page.
 *
 * To ban a user from any service:
 *   redisTemplate.convertAndSend("user:banned", userId.toString());
 *   redisTemplate.opsForValue().set("banned:user:" + userId, "1"); // prevent reconnect
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserBannedEventListener implements MessageListener {

  private static final CloseStatus SUSPENDED_STATUS =
      new CloseStatus(4403, "Account suspended");

  private final WebSocketSessionRegistry sessionRegistry;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    byte[] body = message.getBody();
    if (body == null || body.length == 0) {
      log.warn("Received null/empty body on user:banned channel — skipping");
      return;
    }
    String userId = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
    if (userId.isEmpty()) {
      log.warn("Received blank userId on user:banned channel — skipping");
      return;
    }

    log.info("Ban event received for user {}. Closing all active WebSocket sessions.", userId);

    Set<WebSocketSession> userSessions = sessionRegistry.getSessionsForUser(userId);
    if (userSessions.isEmpty()) {
      log.debug("User {} has no active WebSocket sessions — nothing to close", userId);
      return;
    }

    for (WebSocketSession session : userSessions) {
      try {
        if (session.isOpen()) {
          session.close(SUSPENDED_STATUS);
          log.info("Closed WebSocket session {} for banned user {}", session.getId(), userId);
        }
      } catch (IOException ex) {
        log.error("Failed to close WebSocket session {} for user {}: {}",
            session.getId(), userId, ex.getMessage());
      }
    }
  }
}
