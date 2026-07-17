package com.codeit.team5.mopl.global.infra.redis.config;

import com.codeit.team5.mopl.content.dto.response.ContentResponse;
import com.codeit.team5.mopl.content.store.ContentRatingStats;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    public static final String CONTENT_LIST_CACHE = "content:list";
    public static final String CONTENT_RATING_STATS_CACHE = "contentRatingStats";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "mopl:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()));

        JavaType contentListType = objectMapper.getTypeFactory()
                .constructParametricType(CursorResponse.class, ContentResponse.class);

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        CONTENT_LIST_CACHE, typedConfig(objectMapper, contentListType)
                                .entryTtl(Duration.ofMinutes(1)),
                        CONTENT_RATING_STATS_CACHE, typedConfig(objectMapper, ContentRatingStats.class)
                                .entryTtl(Duration.ofMinutes(1))
                ))
                .build();
    }

    // 캐시 이름마다 반환 타입을 명시해 JSON으로 저장합니다.
    RedisCacheConfiguration typedConfig(ObjectMapper mapper, Class<?> type) {
        return typedConfig(mapper, mapper.getTypeFactory().constructType(type));
    }

    // List<T> 같은 제네릭 타입은 JavaType으로 직접 구성해서 넘깁니다.
    RedisCacheConfiguration typedConfig(ObjectMapper mapper, JavaType type) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "mopl:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(mapper, type)));
    }
}
