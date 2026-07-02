package com.codeit.team5.mopl.sse.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseSenderTest {

    @Mock
    private SseEmitterStore emitterStore;

    @InjectMocks
    private SseSender sseSender;

    // ===== sendToUser =====

    @Test
    @DisplayName("sendToUser: emitter가 있으면 이벤트를 전송한다")
    void sendToUser_sendsEvent_whenEmitterFound() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(emitterStore.get(userId)).willReturn(mockEmitter);

        // when
        sseSender.sendToUser(userId, SseEmitter.event().name("test").data("data"));

        // then
        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendToUser: emitter가 없으면 전송을 건너뛴다")
    void sendToUser_skips_whenNoEmitter() {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.get(userId)).willReturn(null);

        // when
        sseSender.sendToUser(userId, SseEmitter.event().name("test").data("data"));

        // then
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("sendToUser: 전송 실패 시 emitter를 스토어에서 제거한다")
    void sendToUser_removesEmitter_whenSendFails() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(emitterStore.get(userId)).willReturn(mockEmitter);
        doThrow(new IOException("broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        sseSender.sendToUser(userId, SseEmitter.event().name("test").data("data"));

        // then
        verify(emitterStore).remove(eq(userId), eq(mockEmitter));
    }

    // ===== send =====

    @Test
    @DisplayName("send: 전송 성공 시 true를 반환한다")
    void send_returnsTrue_whenSuccess() {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // when
        boolean result = sseSender.send(userId, mockEmitter, SseEmitter.event().name("test").data("data"));

        // then
        assertThat(result).isTrue();
        verify(emitterStore, never()).remove(any(), any());
    }

    @Test
    @DisplayName("send: 전송 실패 시 emitter를 스토어에서 제거하고 false를 반환한다")
    void send_removesEmitterAndReturnsFalse_whenFails() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // when
        boolean result = sseSender.send(userId, mockEmitter, SseEmitter.event().name("test").data("data"));

        // then
        assertThat(result).isFalse();
        verify(emitterStore).remove(eq(userId), eq(mockEmitter));
    }
}
