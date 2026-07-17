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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenStore refreshTokenStore;
    private final AuthMapper authMapper;
    private final AuthSessionService authSessionService;
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

        User user = userRepository.findWithProfileImageById(userId)
                .orElseThrow(() ->
                        new RefreshTokenInvalidException("Invalid refresh token"));

        UserResponse userDto = userMapper.toDto(user);

        Instant expiresAt = calculateExpiresAt();

        String newRefreshToken =
                jwtTokenizer.generateRefreshToken(user.getId().toString());

        // 기존 토큰의 검증과 교체가 성공한 뒤에만 로그인 세션 만료 시간을 연장한다.
        if (!refreshTokenStore.rotateIfValid(
                userId,
                refreshToken,
                newRefreshToken,
                expiresAt
        )) {
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }

        UUID sessionId =
                authSessionService.extendCurrentSession(userId, expiresAt);

        String newAccessToken = jwtTokenizer.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                sessionId.toString()
        );

        JwtResponse jwtResponse =
                authMapper.toJwtResponse(userDto, newAccessToken);

        return authMapper.toAuthPayload(jwtResponse, newRefreshToken);
    }

    private Instant calculateExpiresAt() {
        return Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }
}
