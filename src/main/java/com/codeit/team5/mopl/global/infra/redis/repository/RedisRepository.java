package com.codeit.team5.mopl.global.infra.redis.repository;

import java.util.Optional;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codeit.team5.mopl.global.infra.redis.config.RecordSupportingTypeResolver;

public abstract class RedisRepository<T> {

    protected final RedisTemplate<String, T> redisTemplate;

    protected RedisRepository(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper, Class<T> clazz) {
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());
        
        ObjectMapper mapper = RecordSupportingTypeResolver.createRedisObjectMapper(objectMapper);

        Jackson2JsonRedisSerializer<T> serializer = new Jackson2JsonRedisSerializer<>(mapper, clazz);
        this.redisTemplate.setValueSerializer(serializer);
        this.redisTemplate.afterPropertiesSet();
    }

    protected void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    protected Optional<T> get(String key) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(key));
    }

    protected void delete(String key) {
        redisTemplate.delete(key);
    }
}
