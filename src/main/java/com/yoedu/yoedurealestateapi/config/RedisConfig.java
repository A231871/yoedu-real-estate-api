package com.yoedu.yoedurealestateapi.config;

import com.yoedu.yoedurealestateapi.messaging.UserBannedEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for:
 * 1. RedisTemplate<String, String> — used by WebSocketHandshakeInterceptor
 *    to check the ban blacklist (key: "banned:user:{userId}").
 * 2. RedisMessageListenerContainer — subscribes UserBannedEventListener
 *    to the "user:banned" Pub/Sub channel for real-time session severing.
 *
 * Uses Lettuce as the connection driver (Spring Boot auto-configured).
 * Configuration is driven by spring.data.redis.* properties in application.yaml.
 */
@Configuration
public class RedisConfig {

  /**
   * String-keyed, string-valued RedisTemplate.
   * Used for the ban blacklist and any simple key-value lookups.
   */
  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }

  @Bean
  public org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor redisListenerExecutor() {
    org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor executor = 
        new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("redis-listener-");
    executor.initialize();
    return executor;
  }

  @org.springframework.beans.factory.annotation.Value("${spring.data.redis.listener.auto-startup:true}")
  private boolean autoStartup;

  /**
   * Container that manages the Redis Pub/Sub subscription lifecycle.
   * Subscribes to the "user:banned" channel using PatternTopic (supports wildcards if needed).
   */
  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      UserBannedEventListener userBannedEventListener,
      org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor redisListenerExecutor) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setTaskExecutor(redisListenerExecutor);
    container.addMessageListener(userBannedEventListener, new PatternTopic("user:banned"));
    container.setAutoStartup(autoStartup);
    return container;
  }
}
