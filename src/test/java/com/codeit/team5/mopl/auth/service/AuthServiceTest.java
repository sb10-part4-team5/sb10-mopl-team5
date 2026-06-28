package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.InvalidCredentialsException;
import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

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

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그인에 성공하면 토큰을 발급하고 리프레시 토큰 저장을 요청한다")
    void login_success() {
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
        MoplUserDetails userDetails = new MoplUserDetails(userResponse, "encoded-password");
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SignInRequest request = new SignInRequest("User@Example.COM", "password1");
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        JwtResponse expectedResponse = new JwtResponse(userResponse, accessToken);
        AuthPayload expectedPayload = new AuthPayload(expectedResponse, refreshToken);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenizer.generateAccessToken(userId.toString(), userResponse.email(), userResponse.role()))
                .thenReturn(accessToken);
        when(jwtTokenizer.generateRefreshToken(userId.toString())).thenReturn(refreshToken);
        when(jwtProperties.refreshTokenExpirationMinutes()).thenReturn(420L);
        when(authMapper.toJwtResponse(userResponse, accessToken)).thenReturn(expectedResponse);
        when(authMapper.toAuthPayload(expectedResponse, refreshToken)).thenReturn(expectedPayload);

        Instant before = Instant.now().plus(420, ChronoUnit.MINUTES).minusSeconds(1);

        // When
        AuthPayload result = authService.login(request);

        // Then
        Instant after = Instant.now().plus(420, ChronoUnit.MINUTES).plusSeconds(1);
        assertThat(result).isSameAs(expectedPayload);
        assertThat(result.jwtResponse()).isSameAs(expectedResponse);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        verify(jwtTokenizer).generateAccessToken(userId.toString(), userResponse.email(), userResponse.role());
        verify(jwtTokenizer).generateRefreshToken(userId.toString());
        verify(refreshTokenStore).save(eq(userId), eq(refreshToken), expiresAtCaptor.capture());
        verify(authMapper).toJwtResponse(userResponse, accessToken);
        verify(authMapper).toAuthPayload(expectedResponse, refreshToken);

        UsernamePasswordAuthenticationToken authenticationRequest = authenticationCaptor.getValue();
        assertThat(authenticationRequest.getPrincipal()).isEqualTo("user@example.com");
        assertThat(authenticationRequest.getCredentials()).isEqualTo(request.password());
        assertThat(expiresAtCaptor.getValue()).isBetween(before, after);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패하고 리프레시 토큰을 저장하지 않는다")
    void login_unknownEmail_throwsException() {
        // Given
        SignInRequest request = new SignInRequest("Unknown@Example.COM", "password1");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UserNotFoundException("unknown@example.com"));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UserNotFoundException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtTokenizer, refreshTokenStore, authMapper);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패하고 리프레시 토큰을 저장하지 않는다")
    void login_invalidPassword_throwsException() {
        // Given
        SignInRequest request = new SignInRequest("user@example.com", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new InvalidCredentialsException("비밀번호가 일치하지 않습니다."));

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
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
        when(jwtTokenizer.getRefreshUserId(refreshToken)).thenReturn(userId);

        // When
        authService.logout(refreshToken);

        // Then
        verify(jwtTokenizer).getRefreshUserId(refreshToken);
        verify(refreshTokenStore).deleteByUserId(userId);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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

        // When
        authService.logout(null);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenizer);
        verify(refreshTokenStore, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰이면 로그아웃은 삭제하지 않고 정상 종료한다")
    void logout_invalidRefreshToken_doesNotDeleteRefreshToken() {
        // Given
        String refreshToken = "invalid-refresh-token";
        when(jwtTokenizer.getRefreshUserId(refreshToken))
                .thenThrow(new JwtInvalidException("Invalid refresh token"));

        // When
        authService.logout(refreshToken);

        // Then
        verify(jwtTokenizer).getRefreshUserId(refreshToken);
        verify(refreshTokenStore, never()).deleteByUserId(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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
                .isInstanceOf(JwtInvalidException.class);

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
