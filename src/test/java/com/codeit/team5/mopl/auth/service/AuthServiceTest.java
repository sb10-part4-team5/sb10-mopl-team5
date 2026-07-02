package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.security.handler.signin.SignInSuccessHandler;
import com.codeit.team5.mopl.auth.security.handler.signout.SignOutHandler;
import com.codeit.team5.mopl.auth.security.provider.MoplAuthenticationProvider;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import com.codeit.team5.mopl.auth.service.TemporaryPasswordService;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenCookieManager cookieManager;

    @Mock
    private MoplUserDetailsService userDetailsService;

    @Mock
    private TemporaryPasswordService temporaryPasswordService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그인에 성공하면 토큰을 발급하고 리프레시 토큰 저장을 요청한다")
    void login_success() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(
                userId,
                Instant.parse("2026-06-24T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        MoplUserDetails userDetails = new MoplUserDetails(
                new AuthUser(userResponse.id(), userResponse.email(), userResponse.role(), userResponse.locked()),
                "encoded-password"
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtResponse expectedResponse = new JwtResponse(userResponse, accessToken);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(420 * 60)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);
        when(jwtTokenizer.generateAccessToken(userId.toString(), userResponse.email(), userResponse.role()))
                .thenReturn(accessToken);
        when(jwtTokenizer.generateRefreshToken(userId.toString())).thenReturn(refreshToken);
        when(jwtProperties.refreshTokenExpirationMinutes()).thenReturn(420L);
        when(cookieManager.createCookie(refreshToken)).thenReturn(refreshTokenCookie);
        when(authMapper.toJwtResponse(userResponse, accessToken)).thenReturn(expectedResponse);

        Instant before = Instant.now().plus(420, ChronoUnit.MINUTES).minusSeconds(1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SignInSuccessHandler handler = new SignInSuccessHandler(
                new ObjectMapper().findAndRegisterModules(),
                cookieManager,
                refreshTokenStore,
                authMapper,
                jwtTokenizer,
                jwtProperties,
                userRepository,
                userMapper
        );

        // When
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        // Then
        Instant after = Instant.now().plus(420, ChronoUnit.MINUTES).plusSeconds(1);
        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(jwtTokenizer).generateAccessToken(userId.toString(), userResponse.email(), userResponse.role());
        verify(jwtTokenizer).generateRefreshToken(userId.toString());
        verify(refreshTokenStore).save(eq(userId), eq(refreshToken), expiresAtCaptor.capture());
        verify(cookieManager).createCookie(refreshToken);
        verify(authMapper).toJwtResponse(userResponse, accessToken);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH_TOKEN=refresh-token");
        assertThat(response.getContentAsString()).contains("\"accessToken\":\"access-token\"");
        assertThat(response.getContentAsString()).contains("\"email\":\"user@example.com\"");
        assertThat(expiresAtCaptor.getValue()).isBetween(before, after);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패하고 리프레시 토큰을 저장하지 않는다")
    void login_unknownEmail_throwsException() {
        // Given
        MoplAuthenticationProvider provider = new MoplAuthenticationProvider(
                userDetailsService,
                temporaryPasswordService,
                passwordEncoder
        );
        when(userDetailsService.loadUserByUsername("unknown@example.com"))
                .thenThrow(new UserNotFoundException("unknown@example.com"));
        Authentication authentication =
                UsernamePasswordAuthenticationToken.unauthenticated("unknown@example.com", "password1");

        // When & Then
        assertThatThrownBy(() -> provider.authenticate(authentication))
                .isInstanceOf(UserNotFoundException.class);

        verify(userDetailsService).loadUserByUsername("unknown@example.com");
        verifyNoInteractions(jwtTokenizer, refreshTokenStore, authMapper, temporaryPasswordService);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패하고 리프레시 토큰을 저장하지 않는다")
    void login_invalidPassword_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = new MoplUserDetails(
                new AuthUser(userId, "user@example.com", "USER", false),
                "encoded-password"
        );
        MoplAuthenticationProvider provider = new MoplAuthenticationProvider(
                userDetailsService,
                temporaryPasswordService,
                passwordEncoder
        );
        Authentication authentication =
                UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "wrong-password");

        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
        when(temporaryPasswordService.matches(userId, "wrong-password")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> provider.authenticate(authentication))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(userDetailsService).loadUserByUsername("user@example.com");
        verify(passwordEncoder).matches("wrong-password", "encoded-password");
        verify(temporaryPasswordService).matches(userId, "wrong-password");
        verify(jwtTokenizer, never()).generateAccessToken(any(), any(), any());
        verify(jwtTokenizer, never()).generateRefreshToken(any());
        verifyNoInteractions(refreshTokenStore, authMapper);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 로그아웃하면 사용자 식별자로 리프레시 토큰을 삭제한다")
    void logout_validRefreshToken_success() {
        // Given
        UUID userId = UUID.randomUUID();
        String refreshToken = "valid-refresh-token";
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(0)
                .build();
        when(jwtTokenizer.getRefreshUserId(refreshToken)).thenReturn(userId);
        when(cookieManager.deleteCookie()).thenReturn(deleteCookie);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("REFRESH_TOKEN", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        SignOutHandler handler = new SignOutHandler(jwtTokenizer, refreshTokenStore, cookieManager);

        // When
        handler.logout(request, response, null);

        // Then
        verify(jwtTokenizer).getRefreshUserId(refreshToken);
        verify(refreshTokenStore).deleteByUserId(userId);
        verify(cookieManager).deleteCookie();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH_TOKEN=");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 로그아웃은 리프레시 토큰을 삭제하지 않고 종료한다")
    void logout_missingRefreshToken_doesNotDeleteRefreshToken() {
        // Given
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user@example.com",
                null
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(0)
                .build();
        when(cookieManager.deleteCookie()).thenReturn(deleteCookie);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        SignOutHandler handler = new SignOutHandler(jwtTokenizer, refreshTokenStore, cookieManager);

        // When
        handler.logout(request, response, null);

        // Then
        verifyNoInteractions(jwtTokenizer);
        verify(refreshTokenStore, never()).deleteByUserId(any());
        verify(cookieManager).deleteCookie();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH_TOKEN=");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 로그아웃은 삭제하지 않고 정상 종료한다")
    void logout_invalidRefreshToken_doesNotDeleteRefreshToken() {
        // Given
        String refreshToken = "invalid-refresh-token";
        ResponseCookie deleteCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .path("/api/auth")
                .maxAge(0)
                .build();
        when(jwtTokenizer.getRefreshUserId(refreshToken))
                .thenThrow(new RefreshTokenInvalidException("Invalid refresh token"));
        when(cookieManager.deleteCookie()).thenReturn(deleteCookie);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("REFRESH_TOKEN", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        SignOutHandler handler = new SignOutHandler(jwtTokenizer, refreshTokenStore, cookieManager);

        // When
        handler.logout(request, response, null);

        // Then
        verify(jwtTokenizer).getRefreshUserId(refreshToken);
        verify(refreshTokenStore, never()).deleteByUserId(any());
        verify(cookieManager).deleteCookie();
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("REFRESH_TOKEN=");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 새 토큰을 발급하고 리프레시 토큰을 회전한다")
    void refresh_validRefreshToken_success() {
        // Given
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        UserResponse userResponse = new UserResponse(
                userId,
                Instant.parse("2026-06-24T00:00:00Z"),
                user.getEmail(),
                user.getName(),
                null,
                "USER",
                false
        );
        JwtResponse jwtResponse = new JwtResponse(userResponse, newAccessToken);
        AuthPayload expectedPayload = new AuthPayload(jwtResponse, newRefreshToken);

        when(jwtTokenizer.getRefreshUserId(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);
        when(jwtTokenizer.generateAccessToken(userId.toString(), user.getEmail(), user.getRole().name()))
                .thenReturn(newAccessToken);
        when(jwtTokenizer.generateRefreshToken(userId.toString())).thenReturn(newRefreshToken);
        when(jwtProperties.refreshTokenExpirationMinutes()).thenReturn(420L);
        when(refreshTokenStore.rotateIfValid(
                eq(userId),
                eq(refreshToken),
                eq(newRefreshToken),
                any(Instant.class)
        )).thenReturn(true);
        when(authMapper.toJwtResponse(userResponse, newAccessToken)).thenReturn(jwtResponse);
        when(authMapper.toAuthPayload(jwtResponse, newRefreshToken)).thenReturn(expectedPayload);

        Instant before = Instant.now().plus(420, ChronoUnit.MINUTES).minusSeconds(1);

        // When
        AuthPayload result = authService.refresh(refreshToken);

        // Then
        Instant after = Instant.now().plus(420, ChronoUnit.MINUTES).plusSeconds(1);
        assertThat(result).isSameAs(expectedPayload);

        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(jwtTokenizer).getRefreshUserId(refreshToken);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
        verify(jwtTokenizer).generateAccessToken(userId.toString(), user.getEmail(), user.getRole().name());
        verify(jwtTokenizer).generateRefreshToken(userId.toString());
        verify(refreshTokenStore).rotateIfValid(
                eq(userId),
                eq(refreshToken),
                eq(newRefreshToken),
                expiresAtCaptor.capture()
        );
        verify(refreshTokenStore, never()).existsValidToken(any(), any());
        verify(refreshTokenStore, never()).save(any(), any(), any());
        verify(authMapper).toJwtResponse(userResponse, newAccessToken);
        verify(authMapper).toAuthPayload(jwtResponse, newRefreshToken);
        assertThat(expiresAtCaptor.getValue()).isBetween(before, after);
    }

    @Test
    @DisplayName("리프레시 토큰 회전에 실패하면 토큰 재발급에 실패한다")
    void refresh_rotateIfValidFalse_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        UserResponse userResponse = new UserResponse(
                userId,
                Instant.parse("2026-06-24T00:00:00Z"),
                user.getEmail(),
                user.getName(),
                null,
                "USER",
                false
        );

        when(jwtTokenizer.getRefreshUserId(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);
        when(jwtTokenizer.generateAccessToken(userId.toString(), user.getEmail(), user.getRole().name()))
                .thenReturn(newAccessToken);
        when(jwtTokenizer.generateRefreshToken(userId.toString())).thenReturn(newRefreshToken);
        when(jwtProperties.refreshTokenExpirationMinutes()).thenReturn(420L);
        when(refreshTokenStore.rotateIfValid(
                eq(userId),
                eq(refreshToken),
                eq(newRefreshToken),
                any(Instant.class)
        )).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(RefreshTokenInvalidException.class)
                .hasMessage("Invalid refresh token");

        verify(refreshTokenStore).rotateIfValid(
                eq(userId),
                eq(refreshToken),
                eq(newRefreshToken),
                any(Instant.class)
        );
        verify(refreshTokenStore, never()).existsValidToken(any(), any());
        verify(refreshTokenStore, never()).save(any(), any(), any());
        verifyNoInteractions(authMapper);
    }
}
