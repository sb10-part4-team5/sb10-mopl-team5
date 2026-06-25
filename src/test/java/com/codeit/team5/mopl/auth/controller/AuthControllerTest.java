package com.codeit.team5.mopl.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.filter.JwtAuthenticationFilter;
import com.codeit.team5.mopl.auth.handler.UserAccessDeniedHandler;
import com.codeit.team5.mopl.auth.handler.UserAuthenticationEntryPoint;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.auth.service.AuthService;
import com.codeit.team5.mopl.config.SecurityConfig;
import com.codeit.team5.mopl.global.exception.GlobalExceptionHandler;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        UserAuthenticationEntryPoint.class,
        UserAccessDeniedHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenizer jwtTokenizer;

    @MockitoBean
    private MoplUserDetailsService userDetailsService;

    @MockitoBean
    private MoplAuthenticationProvider moplAuthenticationProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("로그인에 성공하면 accessToken과 사용자 정보를 반환한다")
    void login_success() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "password1");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        JwtResponse response = new JwtResponse(
                new UserResponse(
                        userId,
                        Instant.parse("2026-06-24T00:00:00Z"),
                        request.username(),
                        "사용자",
                        null,
                        "USER",
                        false
                ),
                "access-token"
        );
        given(authService.login(any(SignInRequest.class))).willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.userDto.id").value(userId.toString()))
                .andExpect(jsonPath("$.userDto.email").value(request.username()))
                .andExpect(jsonPath("$.userDto.name").value("사용자"))
                .andExpect(jsonPath("$.userDto.role").value("USER"))
                .andExpect(jsonPath("$.userDto.locked").value(false));

        verify(authService).login(request);
    }

    @Test
    @DisplayName("로그인 요청에서 이메일이 누락되면 400 응답을 반환한다")
    void login_missingEmail_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
                {
                  "password": "password1"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username[0]").value("이메일은 필수입니다."));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 이메일 형식이 올바르지 않으면 400 Bad Request를 반환한다")
    void signIn_invalidEmail_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("invalid-email", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username").isArray())
                .andExpect(jsonPath("$.details.username").isNotEmpty());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 이메일이 공백이면 400 응답을 반환한다")
    void login_blankEmail_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("   ", "password1");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.username").isArray())
                .andExpect(jsonPath("$.details.username").isNotEmpty());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청에서 비밀번호가 누락되면 400 응답을 반환한다")
    void login_missingPassword_returnsBadRequest() throws Exception {
        // Given
        String requestJson = """
                {
                  "username": "user@example.com"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password[0]").value("비밀번호는 필수입니다."));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("로그인 요청의 비밀번호가 공백이면 400 응답을 반환한다")
    void login_blankPassword_returnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "   ");

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionType").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.details.password[0]").value("비밀번호는 필수입니다."));

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("인증 실패 예외가 발생하면 401 인증 실패 응답을 반환한다")
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "wrong-password");
        given(authService.login(any(SignInRequest.class)))
                .willThrow(new InvalidCredentialsException("비밀번호가 일치하지 않습니다."));

        // When & Then
        mockMvc.perform(post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionType").value("InvalidCredentialsException"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService).login(request);
    }

    @Test
    @DisplayName("인증 헤더 없이 로그아웃을 요청하면 401 응답을 반환한다")
    void logout_missingAuthorizationHeader_returnsUnauthorized() throws Exception {
        // Given: Authorization 헤더 없음

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.exceptionName").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(authService, never()).logout();
    }

    @Test
    @DisplayName("인증 헤더가 있으면 로그아웃에 성공한다")
    void logout_authenticatedUser_success() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        mockAccessToken(accessToken);

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        verify(authService).logout();
    }

    @Test
    @DisplayName("로그아웃 서비스 예외가 발생하면 500 서버 오류 응답을 반환한다")
    void logout_serviceException_returnsInternalServerError() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        mockAccessToken(accessToken);
        org.mockito.BDDMockito.willThrow(new IllegalStateException("logout failed"))
                .given(authService)
                .logout();

        // When & Then
        mockMvc.perform(post("/api/auth/sign-out")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionType").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 에러가 발생했습니다."))
                .andExpect(jsonPath("$.details").doesNotExist());

        verify(authService).logout();
    }

    private void mockAccessToken(String accessToken) {
        @SuppressWarnings("unchecked")
        Jws<Claims> claimsJws = org.mockito.Mockito.mock(Jws.class);
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        UserResponse userResponse = new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                Instant.parse("2026-06-24T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        MoplUserDetails userDetails = new MoplUserDetails(userResponse, "encoded-password");
        User user = User.create(userResponse.email(), "encoded-password", userResponse.name());
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", userResponse.id());

        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(userResponse.id().toString());
        given(claims.get("email", String.class)).willReturn("user@example.com");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(userDetails);
    }
}
