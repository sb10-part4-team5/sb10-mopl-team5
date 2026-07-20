package com.codeit.team5.mopl.global.web.ws.stomp.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebSocketSessionStore {

    private static final String KEY_PREFIX = "ws:session:";
    private static final long TTL_HOURS = 24;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketSessionStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    private String getKey(UUID userId) {
        return KEY_PREFIX + userId.toString();
    }

    public void connect(UUID userId) {
        // Redis에서는 Hash 필드 삽입 시 키가 자동 생성되므로 반드시 필요하진 않지만,
        // 연결 유지 목적으로 TTL을 갱신하거나 빈 세션을 나타낼 수 있습니다.
        // 현재 STOMP 핸들러에서 빈 상태 조회가 있을 수 있으므로 연결 시간(TTL) 갱신용으로 처리
        stringRedisTemplate.expire(getKey(userId), TTL_HOURS, TimeUnit.HOURS);
    }

    public void subscribe(UUID userId, String subscriptionId, StompDestination stompDestination) {
        String key = getKey(userId);
        try {
            String value = objectMapper.writeValueAsString(stompDestination);
            stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) {
                    @SuppressWarnings("unchecked")
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.opsForHash().put(key, subscriptionId, value);
                    ops.expire(key, TTL_HOURS, TimeUnit.HOURS);
                    return null;
                }
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize StompDestination", e);
        }
    }

    public void unsubscribe(UUID userId, String subscriptionId) {
        String key = getKey(userId);
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                @SuppressWarnings("unchecked")
                RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                ops.opsForHash().delete(key, subscriptionId);
                ops.expire(key, TTL_HOURS, TimeUnit.HOURS);
                return null;
            }
        });
    }

    public void disconnect(UUID userId) {
        stringRedisTemplate.delete(getKey(userId));
    }

    public Optional<StompDestination> getDestination(UUID userId, String subscriptionId) {
        Object value = stringRedisTemplate.opsForHash().get(getKey(userId), subscriptionId);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value.toString(), StompDestination.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize StompDestination", e);
        }
    }

    // 해당 사용자가 특정 destination을 구독 중인지 (활성 여부 판단)
    public boolean isSubscribed(UUID userId, String destination) {
        return getAllDestination(userId).stream()
            .anyMatch(d -> d.getResolvedDestination().equals(destination));
    }

    public Collection<StompDestination> getAllDestination(UUID userId) {
        List<Object> values = stringRedisTemplate.opsForHash().values(getKey(userId));
        return values.stream().map(v -> {
            try {
                return objectMapper.readValue(v.toString(), StompDestination.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize StompDestination", e);
            }
        }).toList();
    }

    public record StompDestination(String destinationPattern, UUID targetId) {

        @JsonIgnore
        public String getPattern() {
            return destinationPattern.replace("{id}", "*");
        }

        @JsonIgnore
        public String getResolvedDestination() {
            return this.destinationPattern.replace("{id}", this.targetId.toString());
        }
    }
}
