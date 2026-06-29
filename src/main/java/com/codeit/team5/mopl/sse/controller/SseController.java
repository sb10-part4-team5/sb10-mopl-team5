package com.codeit.team5.mopl.sse.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.sse.controller.api.SseApi;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController implements SseApi {

    // SSE 타임아웃 시간
    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;

    private final SseEmitterStore emitterStore;

    @Override
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @RequestParam(required = false) UUID lastEventId) {

        // SecurityContextHolder에 있는 유저 인증 정보로부터 현재 로그인하고 있는 사용자의 id를 추출한다.
        UUID userId = userDetails.getId();
        log.info("SSE subscribe request: GET /api/sse");

        // SseEmitter 객체 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        // 로그인 한 유저의 id를 바탕으로 emitter 객체를 스토리지에 저장함.
        emitterStore.save(userId, emitter);

        // Emitter 연결이 종료되면 객체 정리
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: userId={}", userId);
            emitterStore.remove(userId);
        });
        // 타임아웃이 지나면 객체 정리
        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out: userId={}", userId);
            emitter.complete();
        });
        // 에러 발생 시 객체 정리
        emitter.onError(e -> {
            log.debug("SSE connection error: userId={}", userId);
            emitterStore.remove(userId);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            log.warn("SSE initial event send failed: userId={}", userId);
            emitterStore.remove(userId);
        }

        return emitter;
    }
}
