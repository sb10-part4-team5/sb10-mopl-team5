package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.entity.RefreshToken;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.token.RefreshTokenHasher;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DbRefreshTokenStoreTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DbRefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("리프레시 토큰 원문을 해시로 변환해 저장한다")
    void save_hashesRawTokenAndSaves() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Instant expiresAt = Instant.parse("2026-06-24T12:00:00Z");

        when(refreshTokenHasher.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

        // When
        refreshTokenStore.save(userId, "raw-refresh-token", expiresAt);

        // Then
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenHasher).hash("raw-refresh-token");
        verify(userRepository).findByIdForUpdate(userId);
        verify(refreshTokenRepository).deleteByUser_Id(userId);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo("hashed-refresh-token");
        assertThat(savedToken.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이 있으면 true를 반환한다")
    void existsValidToken_existingToken_returnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        String rawToken = "raw-refresh-token";
        String tokenHash = "hashed-refresh-token";
        User user = createUser(userId);
        RefreshToken refreshToken = RefreshToken.create(user, tokenHash, Instant.now().plusSeconds(60));

        when(refreshTokenHasher.hash(rawToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByUser_IdAndTokenHashAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(tokenHash),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(Optional.of(refreshToken));

        // When
        boolean result = refreshTokenStore.existsValidToken(userId, rawToken);

        // Then
        assertThat(result).isTrue();
        verify(refreshTokenHasher).hash(rawToken);
        verify(refreshTokenRepository).findByUser_IdAndTokenHashAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(tokenHash),
                org.mockito.ArgumentMatchers.any(Instant.class)
        );
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이 없으면 false를 반환한다")
    void existsValidToken_missingToken_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        String rawToken = "raw-refresh-token";
        String tokenHash = "hashed-refresh-token";

        when(refreshTokenHasher.hash(rawToken)).thenReturn(tokenHash);
        when(refreshTokenRepository.findByUser_IdAndTokenHashAndExpiresAtAfter(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(tokenHash),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(Optional.empty());

        // When
        boolean result = refreshTokenStore.existsValidToken(userId, rawToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("유효한 기존 리프레시 토큰이면 새 리프레시 토큰으로 교체하고 true를 반환한다")
    void rotateIfValid_validToken_replacesTokenAndReturnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        String oldTokenHash = "old-hashed-refresh-token";
        String newTokenHash = "new-hashed-refresh-token";
        Instant expiresAt = Instant.parse("2026-06-24T12:00:00Z");
        RefreshToken existingToken = RefreshToken.create(
                user,
                oldTokenHash,
                Instant.parse("2026-06-24T11:00:00Z")
        );

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokenHasher.hash(oldToken)).thenReturn(oldTokenHash);
        when(refreshTokenRepository.findByUser_IdAndTokenHashAndExpiresAtAfter(
                eq(userId),
                eq(oldTokenHash),
                any(Instant.class)
        )).thenReturn(Optional.of(existingToken));
        when(refreshTokenHasher.hash(newToken)).thenReturn(newTokenHash);

        // When
        boolean result = refreshTokenStore.rotateIfValid(userId, oldToken, newToken, expiresAt);

        // Then
        assertThat(result).isTrue();

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(userRepository).findByIdForUpdate(userId);
        verify(refreshTokenHasher).hash(oldToken);
        verify(refreshTokenRepository).findByUser_IdAndTokenHashAndExpiresAtAfter(
                eq(userId),
                eq(oldTokenHash),
                any(Instant.class)
        );
        verify(refreshTokenHasher).hash(newToken);
        verify(refreshTokenRepository).deleteByUser_Id(userId);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(newTokenHash);
        assertThat(savedToken.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("사용자가 없으면 리프레시 토큰 회전은 false를 반환한다")
    void rotateIfValid_missingUser_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        // When
        boolean result = refreshTokenStore.rotateIfValid(
                userId,
                "old-refresh-token",
                "new-refresh-token",
                Instant.parse("2026-06-24T12:00:00Z")
        );

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByIdForUpdate(userId);
        verify(refreshTokenHasher, never()).hash(any());
        verify(refreshTokenRepository, never()).deleteByUser_Id(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("유효한 기존 리프레시 토큰이 없으면 새 토큰을 저장하지 않고 false를 반환한다")
    void rotateIfValid_missingToken_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        String oldTokenHash = "old-hashed-refresh-token";

        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(refreshTokenHasher.hash(oldToken)).thenReturn(oldTokenHash);
        when(refreshTokenRepository.findByUser_IdAndTokenHashAndExpiresAtAfter(
                eq(userId),
                eq(oldTokenHash),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        // When
        boolean result = refreshTokenStore.rotateIfValid(
                userId,
                oldToken,
                newToken,
                Instant.parse("2026-06-24T12:00:00Z")
        );

        // Then
        assertThat(result).isFalse();
        verify(userRepository).findByIdForUpdate(userId);
        verify(refreshTokenHasher).hash(oldToken);
        verify(refreshTokenHasher, never()).hash(newToken);
        verify(refreshTokenRepository, never()).deleteByUser_Id(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 식별자로 리프레시 토큰을 삭제한다")
    void deleteByUserId_success() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        refreshTokenStore.deleteByUserId(userId);

        // Then
        verify(refreshTokenRepository).deleteByUser_Id(userId);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰 삭제를 Repository에 위임한다")
    void deleteExpiredTokens_success() {
        // Given: 만료 기준 시각은 Store 내부에서 현재 시각으로 계산

        // When
        refreshTokenStore.deleteExpiredTokens();

        // Then
        verify(refreshTokenRepository).deleteByExpiresAtBefore(org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    @DisplayName("사용자 식별자가 null이면 저장에 실패한다")
    void save_nullUserId_throwsException() {
        // Given
        Instant expiresAt = Instant.parse("2026-06-24T12:00:00Z");

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.save(null, "raw-token", expiresAt))
                .withMessage("userId must not be null");
    }

    @Test
    @DisplayName("만료 시간이 null이면 저장에 실패한다")
    void save_nullExpiresAt_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();

        // When & Then
        assertThatNullPointerException()
                .isThrownBy(() -> refreshTokenStore.save(userId, "raw-token", null))
                .withMessage("expiresAt must not be null");
    }

    private User createUser(UUID userId) {
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
