package com.yoedu.yoedurealestateapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

/**
 * Type-safe configuration for the WebSocket STOMP configurations.
 * Set app.ws.broker.relay-enabled=true in production to switch from
 * the in-memory broker to an external RabbitMQ/ActiveMQ relay.
 */
@ConfigurationProperties(prefix = "app.ws")
public record AppWsBrokerProperties(
    List<String> allowedOrigins,
    Broker broker
) {
    public record Broker(
        boolean relayEnabled,
        String relayHost,
        int relayPort,
        String relayLogin,
        String relayPasscode
    ) {}
}
