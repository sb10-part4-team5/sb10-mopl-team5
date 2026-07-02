package com.codeit.team5.mopl.auth.security.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import com.codeit.team5.mopl.auth.service.TemporaryPasswordService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MoplAuthenticationProviderTest {

    @Mock
    private MoplUserDetailsService userDetailsService;

    @Mock
    private TemporaryPasswordService temporaryPasswordService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MoplAuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("일반 비밀번호가 틀려도 임시 비밀번호가 일치하면 인증에 성공한다")
    void authenticate_temporaryPassword_success() {
        // Given
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = new MoplUserDetails(
                new AuthUser(userId, "user@example.com", "USER", false),
                "encoded-password"
        );
        Authentication request =
                UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "Temp1234");

        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("Temp1234", "encoded-password")).thenReturn(false);
        when(temporaryPasswordService.matches(userId, "Temp1234")).thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(request);

        // Then
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(userDetails);
        assertThat(result.getCredentials()).isNull();
        verify(passwordEncoder).matches("Temp1234", "encoded-password");
        verify(temporaryPasswordService).matches(userId, "Temp1234");
    }

    @Test
    @DisplayName("일반 비밀번호와 임시 비밀번호가 모두 일치하지 않으면 인증에 실패한다")
    void authenticate_passwordAndTemporaryPasswordMismatch_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        MoplUserDetails userDetails = new MoplUserDetails(
                new AuthUser(userId, "user@example.com", "USER", false),
                "encoded-password"
        );
        Authentication request =
                UsernamePasswordAuthenticationToken.unauthenticated("user@example.com", "Wrong1234");

        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(passwordEncoder.matches("Wrong1234", "encoded-password")).thenReturn(false);
        when(temporaryPasswordService.matches(userId, "Wrong1234")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationProvider.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(passwordEncoder).matches("Wrong1234", "encoded-password");
        verify(temporaryPasswordService).matches(userId, "Wrong1234");
    }
}
