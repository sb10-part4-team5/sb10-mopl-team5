package com.codeit.team5.mopl.sse.sender;

import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// sseEmitter의 send를 호출하는 component
@Component
@RequiredArgsConstructor
@Slf4j
public class SseSender {

    private final SseEmitterStore emitterStore;

    /**
     * userId로 emitter를 조회해 이벤트를 전송한다.
     * emitter가 없으면 조용히 건너뛰고, 전송 실패 시 스토어에서 제거한다.
     * SseListener처럼 "조회 → 전송 → 실패 시 정리"가 한 묶음인 경우 사용한다.
     */
    public void sendToUser(UUID userId, SseEmitter.SseEventBuilder event) {
        SseEmitter emitter = emitterStore.get(userId); // userId를 통해 emitterStore에서 꺼내오기
        if (emitter == null) {
            log.info("[SSE] emitter 없음 — userId={}가 SSE 미연결 상태", userId);
            return;
        }
        log.info("[SSE] emitter 발견, 이벤트 전송 시도: userId={}", userId);
        send(userId, emitter, event);
    }

    /**
     * 이미 확보한 emitter에 이벤트를 전송하고 성공 여부를 반환한다.
     * 실패 시 emitter를 스토어에서 제거한다.
     * SseService처럼 호출부가 실패 후 흐름을 제어해야 하는 경우 사용한다.
     */
    public boolean send(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (Exception e) {
            log.warn("SSE send failed: userId={}", userId);
            emitterStore.remove(userId, emitter); // 전송 실패 시 emitter를 store에서 제거
            return false;
        }
    }
}
