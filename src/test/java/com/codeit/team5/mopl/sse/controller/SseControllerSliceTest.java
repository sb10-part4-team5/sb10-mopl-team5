package com.codeit.team5.mopl.sse.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.TestGlobalExceptionHandlerConfig;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.security.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.security.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInFailureHandler;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.signout.SignOutHandler;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.sse.service.SseService;
import com.codeit.team5.mopl.user.repository.UserRepository;
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
    private SseService sseService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private SignInSuccessHandler signInSuccessHandler;

    @MockitoBean
    private SignInFailureHandler signInFailureHandler;

    @MockitoBean
    private SignOutHandler signOutHandler;

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
        // when + then
        mockMvc.perform(get("/api/sse"))
                .andExpect(status().isUnauthorized()); // 401을 반환

        // 인증이 실패했으므로, 서비스 계층에서 구독은 일어나지 않는다.
        verify(sseService, never()).subscribe(any(), any());
    }

    // ===== 위임 =====

    @Test
    @DisplayName("인증된 요청은 userId를 추출해 SseService.subscribe()에 위임한다")
    void subscribe_delegatesToService_withUserId() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        // sseService.subscribe 호출 시 SseEmitter 반환
        given(sseService.subscribe(eq(userId), isNull())).willReturn(new SseEmitter());

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId))))
                .andExpect(request().asyncStarted());

        // then
        verify(sseService).subscribe(eq(userId), isNull());
    }

    @Test
    @DisplayName("Last-Event-ID 헤더가 있으면 그 값을 SseService.subscribe()에 함께 전달한다")
    void subscribe_passesLastEventId_toService() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String lastEventId = UUID.randomUUID().toString();
        // 이번에는 Last-Event-ID를 포함하여 호출
        given(sseService.subscribe(eq(userId), eq(lastEventId))).willReturn(new SseEmitter());

        // when
        mockMvc.perform(get("/api/sse")
                        .with(authentication(authOf(userId)))
                        .header("Last-Event-ID", lastEventId))
                .andExpect(request().asyncStarted());

        // then
        verify(sseService).subscribe(eq(userId), eq(lastEventId));
    }
}
