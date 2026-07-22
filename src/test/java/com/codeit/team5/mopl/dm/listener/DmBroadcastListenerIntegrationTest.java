package com.codeit.team5.mopl.dm.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.fixture.DirectMessageTestFixtures;
import com.codeit.team5.mopl.dm.provider.DirectMessageBroadcaster;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

// DmBroadcastListener → Redis Pub/Sub → DmRedisMessageSubscriber 왕복 전체를 실제 Redis로 검증한다.
// StompDirectMessageBroadcaster(실제 STOMP 전송)만 mock으로 대체해 수신 여부를 확인한다.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class DmBroadcastListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate tx;

    @MockitoBean
    private DirectMessageBroadcaster directMessageBroadcaster;

    @Test
    @DisplayName("트랜잭션 커밋 후 DirectMessageBroadcastEvent → Redis를 거쳐 브로드캐스트된다")
    void onDirectMessageBroadcast_sendsViaRedis_afterCommit() {
        UUID receiverId = UUID.randomUUID();
        DirectMessageResponse message = DirectMessageTestFixtures.dmMessage(receiverId);

        tx.executeWithoutResult(status ->
                publisher.publishEvent(new DirectMessageBroadcastEvent(message, receiverId)));

        // Redis Pub/Sub 왕복(발행 → DmRedisMessageSubscriber 수신)이 비동기이므로 timeout으로 대기
        ArgumentCaptor<DirectMessageResponse> captor = ArgumentCaptor.forClass(DirectMessageResponse.class);
        verify(directMessageBroadcaster, timeout(5000)).broadcast(captor.capture());
        assertThat(captor.getValue()).isEqualTo(message);
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 브로드캐스트되지 않는다")
    void onDirectMessageBroadcast_doesNotSend_whenRollback() {
        UUID receiverId = UUID.randomUUID();
        DirectMessageResponse message = DirectMessageTestFixtures.dmMessage(receiverId);

        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new DirectMessageBroadcastEvent(message, receiverId));
            status.setRollbackOnly();
        });

        verify(directMessageBroadcaster, after(500).never()).broadcast(any(DirectMessageResponse.class));
    }
}
