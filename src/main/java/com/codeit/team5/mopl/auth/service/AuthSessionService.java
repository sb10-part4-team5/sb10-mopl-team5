package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.exception.SessionInvalidException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthSessionService {

    private final LoginSessionStore loginSessionStore;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UUID getCurrentSessionId(UUID userId) {
        return loginSessionStore.findCurrentSessionId(userId)
                .orElseThrow(() -> new SessionInvalidException("Invalid login session"));
    }

    public UUID replaceUserSession(UUID userId, Instant expiresAt) {
        // 사용자 단위 세션 교체 작업을 직렬화해 동시 로그인 시 중복 세션 생성을 방지한다.
        lockUser(userId);
        invalidateUserSessions(userId);
        return loginSessionStore.save(userId, expiresAt);
    }

    public UUID extendCurrentSession(UUID userId, Instant expiresAt) {
        // 사용자 단위 세션 교체 작업을 직렬화해 동시 로그인 시 중복 세션 생성을 방지한다.
        lockUser(userId);
        return loginSessionStore.extendCurrentSession(userId, expiresAt)
                .orElseThrow(() -> new SessionInvalidException("Invalid login session"));
    }

    @Transactional(readOnly = true)
    public boolean isValidSession(UUID userId, UUID sessionId) {
        return loginSessionStore.isValid(userId, sessionId);
    }

    public void invalidateUserSessions(UUID userId) {
        loginSessionStore.deleteByUserId(userId);
        refreshTokenStore.deleteByUserId(userId);
    }

    public void deleteExpiredSessions() {
        loginSessionStore.deleteExpiredSessions();
        refreshTokenStore.deleteExpiredTokens();
    }

    private void lockUser(UUID userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
