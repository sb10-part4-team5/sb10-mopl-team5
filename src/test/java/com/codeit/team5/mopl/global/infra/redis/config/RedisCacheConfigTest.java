package com.codeit.team5.mopl.global.infra.redis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.user.dto.response.UserSummary;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

class RedisCacheConfigTest {

    private final RedisCacheConfig redisConfig = new RedisCacheConfig();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("typedConfig로 만든 직렬화기는 @class 없이 record를 저장하고 복원 성공")
    void typedConfig_recordRoundTrip_success() {
        RedisCacheConfiguration config = redisConfig.typedConfig(objectMapper, UserSummary.class);
        SerializationPair<Object> pair = config.getValueSerializationPair();

        UserSummary original = new UserSummary(UUID.randomUUID(), "홍길동", "https://cdn.mopl.local/a.png");

        byte[] serialized = toBytes(pair.write(original));
        assertThat(new String(serialized, StandardCharsets.UTF_8)).doesNotContain("@class");

        Object restored = pair.read(ByteBuffer.wrap(serialized));
        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("typedConfig로 만든 직렬화기는 List<record>도 정상 왕복 성공")
    void typedConfig_listOfRecordsRoundTrip_success() {
        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, ConversationResponse.class);
        RedisCacheConfiguration config = redisConfig.typedConfig(objectMapper, listType);
        SerializationPair<Object> pair = config.getValueSerializationPair();

        UserSummary with = new UserSummary(UUID.randomUUID(), "홍길동", "https://cdn.mopl.local/a.png");
        List<ConversationResponse> original = List.of(
                new ConversationResponse(UUID.randomUUID(), with, null, true));

        byte[] serialized = toBytes(pair.write(original));
        assertThat(new String(serialized, StandardCharsets.UTF_8)).doesNotContain("@class");

        Object restored = pair.read(ByteBuffer.wrap(serialized));
        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("캐시 이름별 키 프리픽스가 mopl 네임스페이스로 적용 성공")
    void typedConfig_keyPrefix_success() {
        RedisCacheConfiguration config = redisConfig.typedConfig(objectMapper, UserSummary.class);

        assertThat(config.getKeyPrefixFor("users")).isEqualTo("mopl:users::");
    }

    private byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }
}
