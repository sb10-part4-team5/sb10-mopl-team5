package com.codeit.team5.mopl.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.support.MoplAccountStatusChecker;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationServiceTest {

    @Mock
    private JwtTokenizer jwtTokenizer;

    @Mock
    private MoplUserDetailsService userDetailsService;

    @Mock
    private MoplAccountStatusChecker moplAccountStatusChecker;

    @Mock
    private Jws<Claims> claimsJws;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationService jwtAuthenticationService;

    @Test
    @DisplayName("유효한 access token이면 userId 기반 인증 객체를 생성한다")
    void getAuthentication_success() {
        // Given
        String accessToken = "valid-access-token";
        UUID userId = UUID.randomUUID();
        MoplUserDetails principal = new MoplUserDetails(
                new AuthUser(userId, "user@example.com", "USER", false),
                "encoded-password"
        );

        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(userId.toString());
        given(userDetailsService.loadUserById(userId)).willReturn(principal);

        // When
        Authentication authentication = jwtAuthenticationService.getAuthentication(accessToken);

        // Then
        assertThat(authentication.getPrincipal()).isSameAs(principal);
        assertThat(authentication.getCredentials()).isEqualTo(accessToken);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        verify(userDetailsService).loadUserById(userId);
        verify(moplAccountStatusChecker).check(principal);
    }

    @Test
    @DisplayName("access token subject가 null이면 인증 객체 생성에 실패한다")
    void getAuthentication_nullSubject_throwsException() {
        // Given
        String accessToken = "invalid-access-token";
        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(null);

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
                .isInstanceOf(JwtInvalidException.class)
                .hasMessage("Invalid token subject");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("access token subject가 공백이면 인증 객체 생성에 실패한다")
    void getAuthentication_blankSubject_throwsException() {
        // Given
        String accessToken = "invalid-access-token";
        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(" ");

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
                .isInstanceOf(JwtInvalidException.class)
                .hasMessage("Invalid token subject");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("access token subject가 UUID 형식이 아니면 인증 객체 생성에 실패한다")
    void getAuthentication_invalidUuidSubject_throwsException() {
        // Given
        String accessToken = "invalid-access-token";
        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn("not-uuid");

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
                .isInstanceOf(JwtInvalidException.class)
                .hasMessage("Invalid token subject");
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("잠긴 사용자이면 인증 객체 생성에 실패한다")
    void getAuthentication_lockedUser_throwsException() {
        // Given
        String accessToken = "valid-access-token";
        UUID userId = UUID.randomUUID();
        MoplUserDetails principal = new MoplUserDetails(
                new AuthUser(userId, "locked@example.com", "USER", true),
                "encoded-password"
        );

        given(jwtTokenizer.getAccessClaims(accessToken)).willReturn(claimsJws);
        given(claimsJws.getBody()).willReturn(claims);
        given(claims.getSubject()).willReturn(userId.toString());
        given(userDetailsService.loadUserById(userId)).willReturn(principal);
        doThrow(new LockedException("잠긴 계정입니다."))
                .when(moplAccountStatusChecker)
                .check(principal);

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
                .isInstanceOf(LockedException.class)
                .hasMessage("잠긴 계정입니다.");
        verify(userDetailsService).loadUserById(userId);
        verify(moplAccountStatusChecker).check(principal);
    }

    @Test
    @DisplayName("JwtTokenizer가 JwtException을 던지면 그대로 전파한다")
    void getAuthentication_jwtException_throwsException() {
        // Given
        String accessToken = "invalid-access-token";
        JwtException exception = new JwtException("invalid jwt");
        given(jwtTokenizer.getAccessClaims(accessToken)).willThrow(exception);

        // When & Then
        assertThatThrownBy(() -> jwtAuthenticationService.getAuthentication(accessToken))
                .isSameAs(exception);
        verifyNoInteractions(userDetailsService);
    }
}
