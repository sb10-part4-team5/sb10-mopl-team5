package com.codeit.team5.mopl.sse.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import com.codeit.team5.mopl.sse.emitter.SseEmitterStore;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseEmitterStore emitterStore;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID persistUser(String email) {
        return userRepository.saveAndFlush(User.create(email, "password", "수신자")).getId();
    }

    private Authentication authOf(UUID userId) {
        MoplUserDetails details = new MoplUserDetails(
                new AuthUser(userId, "user@mopl.com", "USER", false), "password");
        return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    }

    // ===== 인증 =====

    @Test
    @DisplayName("인증 없이 SSE 구독하면 401을 반환한다 (SecurityConfig 적용 확인)")
    void subscribe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        // when + then
        mockMvc.perform(get("/api/sse"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 구독 등록 =====

    @Test
    @DisplayName("인증된 사용자가 구독하면 Emitter가 스토어에 등록된다")
    void subscribe_registersEmitterInStore_whenAuthenticated() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted());

        // then
        assertThat(emitterStore.get(userId)).isNotNull();
    }

    @Test
    @DisplayName("같은 사용자가 재구독하면 새 Emitter가 이전 Emitter를 대체한다")
    void subscribe_replacesOldEmitter_whenDuplicateSubscription() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(get("/api/sse").with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted());
        SseEmitter first = emitterStore.get(userId); // 첫 구독 emitter

        mockMvc.perform(get("/api/sse").with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted());
        SseEmitter second = emitterStore.get(userId); // 후속 구독 emitter

        // then: 스토어에는 새 emitter만 남아 있어야 하고, 이전 emitter와 다른 인스턴스여야 한다
        // (first는 complete()가 호출됐지만 Java 참조 자체는 null이 아님 — isNotSameAs로 교체 여부만 검증)
        assertThat(second).isNotNull().isNotSameAs(first);
    }

    // ===== Last-Event-ID 오류 처리 =====

    @Test
    @DisplayName("Last-Event-ID가 UUID 형식이 아니면 Emitter가 스토어에서 제거된다")
    void subscribe_removesEmitterFromStore_whenInvalidLastEventId() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", "invalid-uuid"))
                .andExpect(request().asyncStarted());

        // then
        assertThat(emitterStore.get(userId)).isNull();
    }

    // ===== Last-Event-ID 미수신 이벤트 재전송 =====

    @Test
    @DisplayName("유효한 Last-Event-ID로 재연결하면 미수신 알림을 조회하고 Emitter가 스토어에 남는다")
    void subscribe_withValidLastEventId_fetchesMissedNotificationsAndKeepsEmitter() throws Exception {
        // given
        // notifications.receiver_id → FK → users.id 이므로 User를 먼저 저장
        UUID userId = persistUser("sse-valid-lastid@example.com");

        // 기준 알림: validateLastEventId가 이 ID를 찾아야 통과
        Notification ref = notificationRepository.saveAndFlush(
                Notification.create(userId, NotificationType.FOLLOWED, "기준 알림", "", NotificationLevel.INFO));

        // 미수신 알림: ref 이후에 생성된 알림
        notificationRepository.saveAndFlush(
                Notification.create(userId, NotificationType.FOLLOWED, "미수신 알림", "", NotificationLevel.INFO));

        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", ref.getId().toString()))
                .andExpect(request().asyncStarted());

        // 미수신 이벤트 재전송 성공 → Emitter는 스토어에 유지
        assertThat(emitterStore.get(userId)).isNotNull();
    }

    @Test
    @DisplayName("Last-Event-ID가 DB에 없는 UUID면 유효하지 않은 것으로 판단해 Emitter를 스토어에서 제거한다")
    void subscribe_withUnknownLastEventId_removesEmitter() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", unknownId.toString()))
                .andExpect(request().asyncStarted());

        // then
        // NotificationService.validateLastEventId → InvalidLastEventIdException(BusinessException)
        // → SseService catch(BusinessException) → emitterStore.remove()
        assertThat(emitterStore.get(userId)).isNull();
    }
}
