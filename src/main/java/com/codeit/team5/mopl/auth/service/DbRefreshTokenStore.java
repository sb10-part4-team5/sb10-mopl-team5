package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.entity.RefreshToken;
import com.codeit.team5.mopl.auth.repository.RefreshTokenRepository;
import com.codeit.team5.mopl.auth.support.RefreshTokenHasher;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
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

    @Override
    @Transactional
    public void save(UUID userId, String rawToken, Instant expiresAt) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        String requiredRawToken = Objects.requireNonNull(rawToken, "rawToken must not be null");
        Instant requiredExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        User user = findUserForUpdate(requiredUserId);

        replaceToken(user, requiredRawToken, requiredExpiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsValidToken(UUID userId, String rawToken) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        String requiredRawToken = Objects.requireNonNull(rawToken, "rawToken must not be null");
        String tokenHash = refreshTokenHasher.hash(requiredRawToken);

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
    public boolean rotateIfValid(
            UUID userId,
            String oldToken,
            String newToken,
            Instant expiresAt
    ) {
        UUID requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
        String requiredOldToken = Objects.requireNonNull(oldToken, "oldToken must not be null");
        String requiredNewToken = Objects.requireNonNull(newToken, "newToken must not be null");
        Instant requiredExpiresAt =
                Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        User user = userRepository.findByIdForUpdate(requiredUserId)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (!existsValidToken(requiredUserId, requiredOldToken)) {
            return false;
        }

        replaceToken(user, requiredNewToken, requiredExpiresAt);

        return true;
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

    private User findUserForUpdate(UUID userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void replaceToken(User user, String rawToken, Instant expiresAt) {
        String tokenHash = refreshTokenHasher.hash(rawToken);

        refreshTokenRepository.deleteByUser_Id(user.getId());

        RefreshToken refreshToken = RefreshToken.create(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
    }
}
