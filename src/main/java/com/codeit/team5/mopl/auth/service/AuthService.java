package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenStore refreshTokenStore;
    private final AuthMapper authMapper;

    private final JwtTokenizer jwtTokenizer;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public AuthPayload refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenInvalidException("Refresh token is required");
        }

        UUID userId = jwtTokenizer.getRefreshUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    // refreshToken 안의 userId가 DB에 없다는 뜻이라서 인증 실패로 처리
                    return new RefreshTokenInvalidException("Invalid refresh token");
                });

        UserResponse userDto = userMapper.toDto(user);

        String newAccessToken = jwtTokenizer.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );
        String newRefreshToken = jwtTokenizer.generateRefreshToken(user.getId().toString());
        if (!refreshTokenStore.rotateIfValid(
                userId,
                refreshToken,
                newRefreshToken,
                calculateExpiresAt()
        )) {
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }

        JwtResponse jwtResponse = authMapper.toJwtResponse(userDto, newAccessToken);

        return authMapper.toAuthPayload(jwtResponse, newRefreshToken);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private Instant calculateExpiresAt() {
        return Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }
}
