package com.yoedu.yoedurealestateapi.messaging;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Thread-safe registry mapping each authenticated user to all their active WebSocket sessions.
 *
 * Uses Map<String, Set<WebSocketSession>> so that users with multiple browser tabs
 * (or multiple devices) have all their sessions tracked independently.
 * ConcurrentHashMap.newKeySet() ensures the inner Set is also thread-safe.
 *
 * Populated by the WebSocketHandlerDecoratorFactory registered in WebSocketConfig.
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

  // username → all currently open WebSocket sessions for that user
  private final ConcurrentHashMap<String, Set<WebSocketSession>> sessions =
      new ConcurrentHashMap<>();

  public void register(String username, WebSocketSession session) {
    sessions.compute(username, (key, set) -> {
      if (set == null) {
        set = ConcurrentHashMap.newKeySet();
      }
      set.add(session);
      return set;
    });
    log.debug("Registered WS session {} for user {} (total: {})",
        session.getId(), username, sessions.getOrDefault(username, ConcurrentHashMap.newKeySet()).size());
  }

  public void unregister(String username, WebSocketSession session) {
    sessions.computeIfPresent(username, (key, set) -> {
      set.remove(session);
      log.debug("Unregistered WS session {} for user {}", session.getId(), username);
      return set.isEmpty() ? null : set;
    });
  }

  /**
   * Returns an unmodifiable view of all open sessions for the given user.
   * Returns an empty set if the user has no active sessions.
   */
  public Set<WebSocketSession> getSessionsForUser(String username) {
    return Collections.unmodifiableSet(
        sessions.getOrDefault(username, Collections.emptySet()));
  }
}
