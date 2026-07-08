package com.codeit.team5.mopl.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenizerTest {

    private static final String ACCESS_SECRET_KEY = "12345678901234567890123456789012";
    private static final String REFRESH_SECRET_KEY = "abcdefghijklmnopqrstuvwxyz123456";

    @Test
    @DisplayName("access token 생성 시 sessionId claim을 포함한다")
    void generateAccessToken_includesSessionIdClaim() {
        // Given
        JwtProperties jwtProperties = org.mockito.Mockito.mock(JwtProperties.class);
        JwtTokenizer jwtTokenizer = new JwtTokenizer(jwtProperties);
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(jwtProperties.accessSecretKey()).thenReturn(ACCESS_SECRET_KEY);
        when(jwtProperties.refreshSecretKey()).thenReturn(REFRESH_SECRET_KEY);
        when(jwtProperties.accessTokenExpirationMinutes()).thenReturn(30L);

        // When
        String accessToken = jwtTokenizer.generateAccessToken(
                userId.toString(),
                "user@example.com",
                "USER",
                sessionId.toString()
        );

        // Then
        Claims claims = jwtTokenizer.getAccessClaims(accessToken).getBody();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo("user@example.com");
        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.get("sessionId")).isEqualTo(sessionId.toString());
    }
}
