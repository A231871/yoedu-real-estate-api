package com.yoedu.yoedurealestateapi.config;

import com.yoedu.yoedurealestateapi.messaging.WebSocketSessionRegistry;
import com.yoedu.yoedurealestateapi.security.StompJwtChannelInterceptor;
import com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import static com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor.USERNAME_ATTRIBUTE;
import static com.yoedu.yoedurealestateapi.security.WebSocketHandshakeInterceptor.BAN_KEY_PREFIX;

/**
 * Production-grade STOMP WebSocket configuration.
 *
 * Key design decisions:
 * - NO SockJS: uses native WebSocket only (required for external broker relay).
 * - HTTP-level auth: WebSocketHandshakeInterceptor validates JWT before TCP upgrade.
 * - Multi-tab session tracking: WebSocketHandlerDecoratorFactory populates
 *   WebSocketSessionRegistry so the ban listener can close all a user's tabs.
 * - Configurable broker: in-memory for dev, STOMP broker relay for prod.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompJwtChannelInterceptor stompJwtChannelInterceptor;
  private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
  private final WebSocketSessionRegistry sessionRegistry;
  private final AppWsBrokerProperties brokerProperties;
  private final StringRedisTemplate redisStringTemplate;
  private final ThreadPoolTaskScheduler brokerHeartbeatScheduler;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    if (brokerProperties.broker().relayEnabled()) {
      // ── Production: relay to external RabbitMQ / ActiveMQ broker ──────────
      registry.enableStompBrokerRelay("/topic", "/queue")
          .setRelayHost(brokerProperties.broker().relayHost())
          .setRelayPort(brokerProperties.broker().relayPort())
          .setClientLogin(brokerProperties.broker().relayLogin())
          .setClientPasscode(brokerProperties.broker().relayPasscode())
          .setSystemLogin(brokerProperties.broker().relayLogin())
          .setSystemPasscode(brokerProperties.broker().relayPasscode())
          .setTaskScheduler(brokerHeartbeatScheduler)
          .setSystemHeartbeatReceiveInterval(10000)
          .setSystemHeartbeatSendInterval(10000)
          .setUserDestinationBroadcast("/topic/unresolved-user-destination")
          .setUserRegistryBroadcast("/topic/simp-user-registry");

      log.info("WebSocket: using STOMP broker relay at {}:{}",
          brokerProperties.broker().relayHost(), brokerProperties.broker().relayPort());
    } else {
      // ── Development: in-memory simple broker ──────────────────────────────
      registry.enableSimpleBroker("/topic", "/queue");
      log.info("WebSocket: using in-memory simple broker (relay-enabled=false)");
    }

    // Prefix for @MessageMapping endpoints
    registry.setApplicationDestinationPrefixes("/app");
    // Prefix for user-targeted messages (e.g. /user/{name}/queue/errors)
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    String[] allowed = brokerProperties.allowedOrigins() != null
        ? brokerProperties.allowedOrigins().toArray(String[]::new)
        : new String[]{"http://localhost:*", "http://127.0.0.1:*"};

    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns(allowed)
        .addInterceptors(webSocketHandshakeInterceptor);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // StompJwtChannelInterceptor handles STOMP-level auth (CONNECT → set Principal)
    // and subscription-level authorization (SUBSCRIBE → check conversation participant).
    registration.interceptors(stompJwtChannelInterceptor);
  }

  /**
   * Registers a WebSocketHandlerDecoratorFactory to intercept raw session lifecycle events.
   *
   * This is the correct hook for tracking WebSocketSession objects because:
   * - afterConnectionEstablished fires AFTER the TCP upgrade but BEFORE any STOMP frames.
   * - afterConnectionClosed fires when the socket closes for any reason.
   *
   * The username is available here via session attributes, which were populated
   * by WebSocketHandshakeInterceptor during the HTTP handshake.
   */
  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
      @Override
      public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

          @Override
          public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            super.afterConnectionEstablished(session); // Initialize framework state first
            String username = (String) session.getAttributes().get(USERNAME_ATTRIBUTE);
            if (username != null) {
              sessionRegistry.register(username, session);
              Boolean isBanned = redisStringTemplate.hasKey(BAN_KEY_PREFIX + username);
              if (Boolean.TRUE.equals(isBanned)) {
                sessionRegistry.unregister(username, session);
                session.close(new CloseStatus(4403, "Account suspended"));
                log.warn("Closed WebSocket connection established during ban transition for user {}", username);
              }
            }
          }

          @Override
          public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
              throws Exception {
            String username = (String) session.getAttributes().get(USERNAME_ATTRIBUTE);
            if (username != null) {
              sessionRegistry.unregister(username, session);
            }
            super.afterConnectionClosed(session, closeStatus);
          }
        };
      }
    });
  }
}
