package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.entity.RefreshToken;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.token.RefreshTokenHasher;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DbRefreshTokenStore implements RefreshTokenStore {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void save(UUID userId, String rawToken, Instant expiresAt) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        String tokenHash = refreshTokenHasher.hash(rawToken);
        Instant requiredExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        User user = userRepository.findByIdForUpdate(requiredUserId)
                .orElseThrow(() -> new UserNotFoundException(requiredUserId));

        refreshTokenRepository.deleteByUser_Id(requiredUserId);

        RefreshToken refreshToken =
                RefreshToken.create(user, tokenHash, requiredExpiresAt);

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsValidToken(UUID userId, String rawToken) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        String tokenHash = refreshTokenHasher.hash(rawToken);

        return refreshTokenRepository
                .findByUser_IdAndTokenHashAndExpiresAtAfter(
                        requiredUserId,
                        tokenHash,
                        Instant.now()
                )
                .isPresent();
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        refreshTokenRepository.deleteByUser_Id(
                Objects.requireNonNull(userId, "userId must not be null")
        );
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
