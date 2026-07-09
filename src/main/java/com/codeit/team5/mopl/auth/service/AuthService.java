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

        // refreshToken 안의 userId가 DB에 없다는 뜻이라서 인증 실패로 처리
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RefreshTokenInvalidException(
                        "Invalid refresh token: user not found - userId=" + userId));

        UserResponse userDto = userMapper.toDto(user);

        Instant expiredAt = calculateExpiresAt();

        UUID sessionId = authSessionService.extendCurrentSession(userId, expiredAt);

        String newAccessToken = jwtTokenizer.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                sessionId.toString()
        );
        String newRefreshToken = jwtTokenizer.generateRefreshToken(user.getId().toString());
        if (!refreshTokenStore.rotateIfValid(
                userId,
                refreshToken,
                newRefreshToken,
                expiredAt
        )) {
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }

        JwtResponse jwtResponse = authMapper.toJwtResponse(userDto, newAccessToken);

        return authMapper.toAuthPayload(jwtResponse, newRefreshToken);
    }

    private Instant calculateExpiresAt() {
        return Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }
}
