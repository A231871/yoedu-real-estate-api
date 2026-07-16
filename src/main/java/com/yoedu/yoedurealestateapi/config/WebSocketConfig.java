package com.yoedu.yoedurealestateapi.config;

import com.yoedu.yoedurealestateapi.security.StompJwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // In-memory broker for topic subscriptions
    registry.enableSimpleBroker("/topic");
    // Prefix for @MessageMapping endpoints
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
        .withSockJS();
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // Authenticate every inbound STOMP frame using our JWT interceptor
    registration.interceptors(stompJwtChannelInterceptor);
  }
}
