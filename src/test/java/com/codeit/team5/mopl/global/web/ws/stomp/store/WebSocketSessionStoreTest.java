package com.codeit.team5.mopl.global.web.ws.stomp.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WebSocketSessionStoreTest {

    private WebSocketSessionStore store;

    @BeforeEach
    void setUp() {
        store = new WebSocketSessionStore();
    }

    @Test
    @DisplayName("connect 호출 시 빈 세션 맵이 생성된다_성공")
    void connect_성공() {
        // given
        String email = "test@test.com";

        // when
        store.connect(email);

        // then
        assertThat(store.getDestination(email, "sub-1")).isNull();
    }

    @Test
    @DisplayName("subscribe 호출 시 세션에 구독 정보가 저장된다_성공")
    void subscribe_성공() {
        // given
        String email = "test@test.com";
        store.connect(email);

        // when
        store.subscribe(email, "sub-1", "/topic/content/1");

        // then
        assertThat(store.getDestination(email, "sub-1")).isEqualTo("/topic/content/1");
    }

    @Test
    @DisplayName("unsubscribe 호출 시 세션에서 구독 정보가 삭제된다_성공")
    void unsubscribe_성공() {
        // given
        String email = "test@test.com";
        store.connect(email);
        store.subscribe(email, "sub-1", "/topic/content/1");

        // when
        store.unsubscribe(email, "sub-1");

        // then
        assertThat(store.getDestination(email, "sub-1")).isNull();
    }

    @Test
    @DisplayName("disconnect 호출 시 유저의 모든 정보가 삭제된다_성공")
    void disconnect_성공() {
        // given
        String email = "test@test.com";
        store.connect(email);
        store.subscribe(email, "sub-1", "/topic/content/1");

        // when
        store.disconnect(email);

        // then
        assertThat(store.getDestination(email, "sub-1")).isNull();
    }

    @Test
    @DisplayName("이미 존재하는 세션에 대해 다시 connect를 호출해도 기존 구독 정보가 유지된다_성공")
    void connect_ExistingSession_성공() {
        // given
        String email = "test@test.com";
        store.connect(email);
        store.subscribe(email, "sub-1", "/topic/content/1");

        // when
        store.connect(email);

        // then
        assertThat(store.getDestination(email, "sub-1")).isEqualTo("/topic/content/1");
    }

    @Test
    @DisplayName("멀티스레드 환경에서 안전하게 동작한다_성공")
    void threadSafety_성공() throws InterruptedException {
        // given
        String email = "test@test.com";
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
                        store.subscribe(email, "sub-" + index, "/topic/content/" + index);
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
            assertThat(store.getDestination(email, "sub-" + i))
                    .isEqualTo("/topic/content/" + i);
        }
    }
}
