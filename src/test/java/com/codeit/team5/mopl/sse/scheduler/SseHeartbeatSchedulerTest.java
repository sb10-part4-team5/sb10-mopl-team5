package com.codeit.team5.mopl.sse.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseHeartbeatSchedulerTest {

    @Mock
    private SseEmitterStore emitterStore;

    @Mock
    private SseSender sseSender;

    @InjectMocks
    private SseHeartbeatScheduler heartbeatScheduler;

    @Test
    @DisplayName("연결된 모든 emitter에 하트비트 이벤트를 전송한다")
    void sendHeartbeat_sendsToAllConnectedEmitters() {
        // given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        SseEmitter emitter1 = mock(SseEmitter.class);
        SseEmitter emitter2 = mock(SseEmitter.class);
        given(emitterStore.getAll()).willReturn(Map.of(userId1, emitter1, userId2, emitter2));

        // when
        heartbeatScheduler.sendHeartbeat();

        // then
        verify(sseSender).send(eq(userId1), eq(emitter1), any(SseEmitter.SseEventBuilder.class));
        verify(sseSender).send(eq(userId2), eq(emitter2), any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("연결된 emitter가 없으면 아무것도 전송하지 않는다")
    void sendHeartbeat_sendsNothing_whenNoEmitterConnected() {
        // given
        given(emitterStore.getAll()).willReturn(Map.of());

        // when
        heartbeatScheduler.sendHeartbeat();

        // then
        verify(sseSender, never()).send(any(), any(), any());
    }
}
