package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.token.RefreshTokenHasher;
import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({
        JpaAuditingConfig.class,
        TestcontainersConfiguration.class,
        DbRefreshTokenStore.class,
        RefreshTokenHasher.class
})
class DbRefreshTokenStoreIntegrationTest {

    @Autowired
    private DbRefreshTokenStore refreshTokenStore;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("같은 사용자 식별자로 리프레시 토큰을 다시 저장하면 이전 토큰은 무효화되고 새 토큰만 유효하다")
    void save_sameUserTwice_invalidatesOldTokenAndKeepsLatestToken() {
        // Given
        User user = userRepository.saveAndFlush(
                User.create("refresh-store@example.com", "encoded-password", "사용자")
        );
        String oldRefreshToken = "old-refresh-token";
        String newRefreshToken = "new-refresh-token";
        Instant expiresAt = Instant.now().plus(420, ChronoUnit.MINUTES);

        // When
        refreshTokenStore.save(user.getId(), oldRefreshToken, expiresAt);
        refreshTokenStore.save(user.getId(), newRefreshToken, expiresAt);

        // Then
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenStore.existsValidToken(user.getId(), oldRefreshToken)).isFalse();
        assertThat(refreshTokenStore.existsValidToken(user.getId(), newRefreshToken)).isTrue();
    }
}
