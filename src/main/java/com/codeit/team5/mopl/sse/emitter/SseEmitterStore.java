package com.codeit.team5.mopl.sse.emitter;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterStore {

    // emitter 객체를 담을 스토리지 <수신자id, emitter>
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    // UserId를 키로 Emitter 저장한다.
    // 동일한 userId의 Emitter가 이미 존재하면 기존 Emitter를 반환하고, 없으면 null 반환
    public SseEmitter save(UUID userId, SseEmitter emitter) {
        return emitters.put(userId, emitter);
    }

    // 수신자 ID에 해당하는 emitter 객체를 가져옴
    public SseEmitter get(UUID userId) {
        return emitters.get(userId);
    }

    // 수신자 ID에 해당하는 emitter 객체를 삭제함
    public void remove(UUID userId, SseEmitter emitter) {
        emitters.remove(userId, emitter);
    }

    // 현재 연결된 모든 emitter를 조회 (하트비트 브로드캐스트 등에 사용)
    public Map<UUID, SseEmitter> getAll() {
        return Collections.unmodifiableMap(emitters);
    }
}
