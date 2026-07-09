package com.codeit.team5.mopl.sse.scheduler;

import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.sse.sender.SseSender;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ALB idle timeout(기본 60초)보다 짧은 주기로 빈 이벤트를 보내 SSE 커넥션이 유휴 상태로
 * 끊기지 않게 유지한다. ALB가 조용히 끊으면 서버는 계속 살아있다고 착각하고, 클라이언트는
 * EventSource가 재연결을 반복하게 된다.
 */
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final SseEmitterStore emitterStore;
    private final SseSender sseSender;

    @Scheduled(fixedRate = 30_000)
    public void sendHeartbeat() {
        for (Map.Entry<UUID, SseEmitter> entry : emitterStore.getAll().entrySet()) {
            sseSender.send(entry.getKey(), entry.getValue(),
                    SseEmitter.event().comment("heartbeat"));
        }
    }
}
