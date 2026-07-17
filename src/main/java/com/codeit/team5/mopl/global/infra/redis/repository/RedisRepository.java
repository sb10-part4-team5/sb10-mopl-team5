package com.codeit.team5.mopl.global.infra.redis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public abstract class RedisRepository<T> {

    protected final RedisTemplate<String, T> redisTemplate;

    protected RedisRepository(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper,
            Class<T> clazz) {
        this.redisTemplate = new RedisTemplate<>();
        this.redisTemplate.setConnectionFactory(connectionFactory);
        this.redisTemplate.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<T> serializer = new Jackson2JsonRedisSerializer<>(objectMapper,
                clazz);

        this.redisTemplate.setValueSerializer(serializer);
        this.redisTemplate.afterPropertiesSet();
    }

    protected void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    protected void set(String key, T value, long timeout, java.util.concurrent.TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    protected Optional<T> get(String key) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(key));
    }

    protected void delete(String key) {
        redisTemplate.delete(key);
    }
}
