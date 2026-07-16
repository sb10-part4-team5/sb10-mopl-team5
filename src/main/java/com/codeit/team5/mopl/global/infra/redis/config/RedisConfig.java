package com.codeit.team5.mopl.global.infra.redis.config;

import java.time.Duration;
import java.util.Map;
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
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))  // 기본 TTL 10분
                .disableCachingNullValues()       // null 값 캐싱 방지
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        redisJsonSerializer(objectMapper)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(Map.of(
                        "sessions", config.entryTtl(Duration.ofMinutes(30)),
                        "users", config.entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }

    private GenericJackson2JsonRedisSerializer redisJsonSerializer(ObjectMapper originalMapper) {
        ObjectMapper mapper = RecordSupportingTypeResolver.createRedisObjectMapper(originalMapper);

        return new GenericJackson2JsonRedisSerializer(mapper);
    }
}
