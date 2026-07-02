package com.codeit.team5.mopl.sse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.sse.dto.DirectMessagePayload;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SseController.class)
@Import({
        GlobalExceptionHandler.class,
        TestGlobalExceptionHandlerConfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class SseControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseEmitterStore emitterStore;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    // userId를 기반으로 테스트용 Authentication 객체를 생성한다.
    private Authentication authOf(UUID userId) {
        MoplUserDetails details = new MoplUserDetails(
                new AuthUser(userId, "user@mopl.com", "USER", false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ===== 인증 =====

    @Test
    @DisplayName("인증 없이 SSE 구독하면 401을 반환한다")
    void subscribe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        // when + then
        mockMvc.perform(get("/api/sse"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
        verify(emitterStore, never()).save(any(), any());
    }

    // ===== 정상 구독 =====

    @Test
    @DisplayName("구독 시 Emitter를 스토어에 저장하고 SSE 스트림을 시작한다")
    void subscribe_savesEmitterToStore_andStartsStream() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        // when + then
        // 인증 정보와 함께 /api/sse 엔드포인트를 통해 구독 요청 수행
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted()); // 해당 요청이 비동기 모드로 시작되었는가?

        // emitterStore에 k: userId, v:SseEmitter(아무객체나) 저장 확인
        verify(emitterStore).save(eq(userId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("기존 구독자가 있으면 이전 Emitter를 complete 처리하고 새 Emitter로 교체한다")
    void subscribe_completedPreviousEmitter_whenDuplicateSubscription() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        SseEmitter previousEmitter = mock(SseEmitter.class); // 이전 Emitter
        given(emitterStore.save(eq(userId), any())).willReturn(previousEmitter);

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted());

        // then (이전 emitter가 정리되었는가?)
        verify(previousEmitter).complete();
    }

    // ===== Last-Event-ID (미수신 이벤트 재전송) =====

    @Test
    @DisplayName("유효한 Last-Event-ID가 있으면 미수신 알림과 DM을 조회하여 재전송한다")
    void subscribe_withValidLastEventId_queriesMissedEvents() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        // 미수신 알림/DM 조회 결과 = 빈 리스트
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of());

        // when (브라우저에서 Last-Event-ID 헤더 포함 요청 전송 시, 받지 못한 정보가 있다는 것)
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted());

        // then
        verify(notificationService).findMissedNotifications(eq(userId), eq(lastEventId));
        verify(notificationService).findMissedDirectMessages(eq(userId), eq(lastEventId));
    }

    @Test
    @DisplayName("미수신 알림이 있으면 notifications 이벤트 전송을 시도한다")
    void subscribe_withMissedNotifications_queriesNotificationService() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        // 미수신 알림 페이로드 객체
        NotificationPayload missed = new NotificationPayload(
                UUID.randomUUID(), userId, NotificationType.FOLLOWED,
                "팔로우 알림", "누군가 팔로우했어요", NotificationLevel.INFO, Instant.now());
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        // 미수신 객체조회 시 missed 가 들어있는 리스트 반환
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of(missed));
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of());

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted());

        // then
        verify(notificationService).findMissedNotifications(eq(userId), eq(lastEventId));
    }

    @Test
    @DisplayName("미수신 DM이 있으면 direct-messages 이벤트 전송을 시도한다")
    void subscribe_withMissedDms_queriesDirectMessageService() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DirectMessagePayload missedDm = new DirectMessagePayload(
                UUID.randomUUID(), userId, "안녕하세요", Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId)).willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId)).willReturn(List.of(missedDm));

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted());

        // then
        verify(notificationService).findMissedDirectMessages(eq(userId), eq(lastEventId));
    }

    // ===== Last-Event-ID 오류 처리 =====

    @Test
    @DisplayName("Last-Event-ID가 UUID 형식이 아니면 Emitter를 제거하고 연결을 종료한다")
    void subscribe_withInvalidLastEventId_removesEmitter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", "not-a-uuid"))
                .andExpect(request().asyncStarted());

        // then
        verify(emitterStore).remove(eq(userId), any(SseEmitter.class));
        verify(notificationService, never()).findMissedNotifications(any(), any());
    }
}
