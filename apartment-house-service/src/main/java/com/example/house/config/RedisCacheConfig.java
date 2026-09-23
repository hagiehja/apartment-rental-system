package com.example.house.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置
 * 支持 LocalDateTime 序列化，并在 JSON 中写入类型信息（@class 字段）
 * 以保证反序列化时能还原为正确的 Java 对象，而非 LinkedHashMap
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

        private GenericJackson2JsonRedisSerializer buildJsonSerializer() {
                ObjectMapper objectMapper = new ObjectMapper();

                // 1. 支持 Java 8 日期类型（LocalDateTime 等）
                objectMapper.registerModule(new JavaTimeModule());
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                // 2. 关键修复：启用多态类型信息，写入 @class 字段
                // 这样反序列化时 Jackson 才能知道目标类型，而不是返回 LinkedHashMap
                BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .build();
                objectMapper.activateDefaultTypingAsProperty(
                                typeValidator,
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                "@class");

                return new GenericJackson2JsonRedisSerializer(objectMapper);
        }

        @Bean
        public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .serializeKeysWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(jsonSerializer))
                                .disableCachingNullValues();

                Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
                // 房源详情缓存 10 分钟
                cacheConfigurations.put("house:detail", defaultConfig.entryTtl(Duration.ofMinutes(10)));
                // 房源列表缓存 5 分钟
                cacheConfigurations.put("house:list", defaultConfig.entryTtl(Duration.ofMinutes(5)));

                return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}
