package com.codeit.team5.mopl.global.web.ws.stomp.store;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.codeit.team5.mopl.TestcontainersConfiguration;

@DataRedisTest
@Import({TestcontainersConfiguration.class, WebSocketSessionStore.class, JacksonAutoConfiguration.class})
class WebSocketSessionStoreTest {

    @Autowired
    private WebSocketSessionStore store;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.execute((RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("connect 호출 시 빈 세션 맵이 생성된다_성공")
    void connect_성공() {
        // given
        UUID email = UUID.randomUUID();

        // when
        store.connect(email);

        // then
        // Redis 구현에서는 connect만으로 빈 Map이 눈에 띄게 추가되진 않지만,
        // 키가 생성되거나 TTL이 갱신되어야 하며 구독 목록은 비어있어야 함
        assertThat(store.getDestination(email, "sub-1")).isEmpty();
        assertThat(store.getAllDestination(email)).isEmpty();
    }

    @Test
    @DisplayName("subscribe 호출 시 세션에 구독 정보가 저장된다_성공")
    void subscribe_성공() {
        // given
        UUID email = UUID.randomUUID();
        store.connect(email);

        UUID targetId = UUID.randomUUID();
        WebSocketSessionStore.StompDestination dest =
                new WebSocketSessionStore.StompDestination("/topic/content/{id}", targetId);
        // when
        store.subscribe(email, "sub-1", dest);

        // then
        assertThat(store.getDestination(email, "sub-1")).contains(dest);
    }

    @Test
    @DisplayName("unsubscribe 호출 시 세션에서 구독 정보가 삭제된다_성공")
    void unsubscribe_성공() {
        // given
        UUID email = UUID.randomUUID();
        store.connect(email);
        UUID targetId = UUID.randomUUID();
        WebSocketSessionStore.StompDestination dest =
                new WebSocketSessionStore.StompDestination("/topic/content/{id}", targetId);
        store.subscribe(email, "sub-1", dest);

        // when
        store.unsubscribe(email, "sub-1");

        // then
        assertThat(store.getDestination(email, "sub-1")).isEmpty();
    }

    @Test
    @DisplayName("disconnect 호출 시 유저의 모든 정보가 삭제된다_성공")
    void disconnect_성공() {
        // given
        UUID email = UUID.randomUUID();
        store.connect(email);
        UUID targetId = UUID.randomUUID();
        WebSocketSessionStore.StompDestination dest =
                new WebSocketSessionStore.StompDestination("/topic/content/{id}", targetId);
        store.subscribe(email, "sub-1", dest);

        // when
        store.disconnect(email);

        // then
        assertThat(store.getDestination(email, "sub-1")).isEmpty();
        assertThat(store.getAllDestination(email)).isEmpty();
    }

    @Test
    @DisplayName("이미 존재하는 세션에 대해 다시 connect를 호출해도 기존 구독 정보가 유지된다_성공")
    void connect_ExistingSession_성공() {
        // given
        UUID email = UUID.randomUUID();
        store.connect(email);
        UUID targetId = UUID.randomUUID();
        WebSocketSessionStore.StompDestination dest =
                new WebSocketSessionStore.StompDestination("/topic/content/{id}", targetId);
        store.subscribe(email, "sub-1", dest);

        // when
        store.connect(email);

        // then
        assertThat(store.getDestination(email, "sub-1")).contains(dest);
    }

    @Test
    @DisplayName("멀티스레드 환경에서 안전하게 동작한다_성공")
    void threadSafety_성공() throws InterruptedException {
        // given
        UUID email = UUID.randomUUID();
        store.connect(email);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        try {
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        store.subscribe(email, "sub-" + index,
                                new WebSocketSessionStore.StompDestination("/topic/content/{id}", email));
                    } finally {
                        latch.countDown();
                    }
                });
            }
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).as("스레드 작업이 5초 안에 완료되지 않음").isTrue();
        } finally {
            executorService.shutdown();
        }

        // then
        for (int i = 0; i < threadCount; i++) {
            assertThat(store.getDestination(email, "sub-" + i).get().getResolvedDestination())
                    .isEqualTo("/topic/content/" + email);
        }
    }
}
