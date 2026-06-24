package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthMapper authMapper;

    private final JwtTokenizer jwtTokenizer;
    private final JwtProperties jwtProperties;

    @Transactional
    public JwtResponse login(SignInRequest request) {
        User user = findByEmailOrElseThrow(normalizeEmail(request.username()));
        String encodedPassword = passwordEncoder.encode(request.password());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ErrorCode.INVALID_PASSWORD);
        }

        Map<String, Object> claims = Map.of(
                "email", user.getEmail(),
                "role", user.getRole().name()
        );

        String subject = user.getId().toString();

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject);
        String refreshToken = jwtTokenizer.generateRefreshToken(user.getId().toString());
        Instant refreshExpiresAt = Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
        refreshTokenStore.save(user.getId(), refreshToken, refreshExpiresAt);

        return authMapper.toDto(user, accessToken);
    }

    public void logout() {

    }

    private User findByEmailOrElseThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
