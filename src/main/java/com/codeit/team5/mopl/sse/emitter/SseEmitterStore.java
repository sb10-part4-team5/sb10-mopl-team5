package com.codeit.team5.mopl.sse.emitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterStore {

    // emitter 객체를 담을 스토리지 <수신자id, emitter>
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // Emitter 객체를 emitters에 저장
    public void save(UUID userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
    }

    // 수신자 ID에 해당하는 emitter 객체를 가져옴
    public SseEmitter get(UUID userId) {
        return emitters.get(userId);
    }

    // 수신자 ID에 해당하는 emitter 객체를 삭제함
    public void remove(UUID userId) {
        emitters.remove(userId);
    }
}
