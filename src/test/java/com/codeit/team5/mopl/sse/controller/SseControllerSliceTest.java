package com.codeit.team5.mopl.sse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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

    private Authentication authOf(UUID userId) {
        MoplUserDetails details = new MoplUserDetails(
                new AuthUser(userId, "user@mopl.com", "USER", false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ===== 인증 =====

    @Test
    @DisplayName("인증 없이 SSE 구독하면 401을 반환한다")
    void subscribe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/sse"))
                .andExpect(status().isUnauthorized());

        verify(emitterStore, never()).save(any(), any());
    }

    // ===== 정상 구독 =====

    @Test
    @DisplayName("Last-Event-ID 없이 구독하면 connect 이벤트를 포함한 SSE 스트림을 반환한다")
    void subscribe_returnsConnectEvent_whenNoLastEventId() throws Exception {
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        MvcResult result = mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(Matchers.containsString("event:connect")))
                .andExpect(content().string(Matchers.containsString("data:connected")));

        verify(emitterStore).save(eq(userId), any(SseEmitter.class));
    }

    @Test
    @DisplayName("기존 구독자가 있으면 이전 Emitter를 complete 처리하고 새 Emitter로 교체한다")
    void subscribe_completedPreviousEmitter_whenDuplicateSubscription() throws Exception {
        UUID userId = UUID.randomUUID();
        SseEmitter previousEmitter = mock(SseEmitter.class);
        given(emitterStore.save(eq(userId), any())).willReturn(previousEmitter);

        MvcResult result = mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());

        verify(previousEmitter).complete();
    }

    // ===== Last-Event-ID (미수신 이벤트 재전송) =====

    @Test
    @DisplayName("유효한 Last-Event-ID가 있으면 미수신 알림과 DM을 조회하여 재전송한다")
    void subscribe_withValidLastEventId_queriesMissedEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of());

        MvcResult result = mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());

        verify(notificationService).findMissedNotifications(eq(userId), eq(lastEventId));
        verify(notificationService).findMissedDirectMessages(eq(userId), eq(lastEventId));
    }

    @Test
    @DisplayName("미수신 알림이 있으면 notifications 이벤트로 재전송한다")
    void subscribe_withMissedNotifications_sendsNotificationEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        NotificationPayload missedNotification = new NotificationPayload(
                UUID.randomUUID(), userId, NotificationType.FOLLOWED,
                "팔로우 알림", "누군가 팔로우했어요", NotificationLevel.INFO, Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of(missedNotification));
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of());

        MvcResult result = mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("event:notifications")))
                .andExpect(content().string(
                        Matchers.containsString(missedNotification.notificationId().toString())));
    }

    @Test
    @DisplayName("미수신 DM이 있으면 direct-messages 이벤트로 재전송한다")
    void subscribe_withMissedDms_sendsDmEvents() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        DirectMessagePayload missedDm = new DirectMessagePayload(
                UUID.randomUUID(), userId, "안녕하세요", Instant.now());

        given(emitterStore.save(eq(userId), any())).willReturn(null);
        given(notificationService.findMissedNotifications(userId, lastEventId))
                .willReturn(List.of());
        given(notificationService.findMissedDirectMessages(userId, lastEventId))
                .willReturn(List.of(missedDm));

        MvcResult result = mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId.toString()))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("event:direct-messages")))
                .andExpect(content().string(Matchers.containsString(missedDm.id().toString())));
    }

    // ===== Last-Event-ID 오류 처리 =====

    @Test
    @DisplayName("Last-Event-ID가 UUID 형식이 아니면 Emitter를 제거하고 연결을 종료한다")
    void subscribe_withInvalidLastEventId_removesEmitter() throws Exception {
        UUID userId = UUID.randomUUID();
        given(emitterStore.save(eq(userId), any())).willReturn(null);

        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", "not-a-uuid"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 잘못된 Last-Event-ID → InvalidLastEventIdException → emitterStore.remove() 호출
        verify(emitterStore).remove(eq(userId), any(SseEmitter.class));
        verify(notificationService, never()).findMissedNotifications(any(), any());
    }
}
