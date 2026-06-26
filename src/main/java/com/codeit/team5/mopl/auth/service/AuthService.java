package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthMapper authMapper;

    private final JwtTokenizer jwtTokenizer;
    private final JwtProperties jwtProperties;

    @Transactional
    public JwtResponse login(SignInRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizeEmail(request.username()),
                        request.password()
                )
        );

        MoplUserDetails principals = (MoplUserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenizer.generateAccessToken(
                principals.getUserDto().id().toString(),
                principals.getUserDto().email(),
                principals.getUserDto().role()
        );
        String refreshToken = jwtTokenizer.generateRefreshToken(principals.getUserDto().id().toString());
        Instant refreshExpiresAt = Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
        refreshTokenStore.save(principals.getUserDto().id(), refreshToken, refreshExpiresAt);

        log.info("Login success: id={}", principals.getUserDto().id());

        return authMapper.toDto(principals.getUserDto(), accessToken);
    }

    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof MoplUserDetails)) {
            log.warn("Logout failed: unauthenticated request");

            throw new JwtInvalidException("Authentication required");
        }

        MoplUserDetails principal = (MoplUserDetails) authentication.getPrincipal();
        UUID userId = principal.getId();

        refreshTokenStore.deleteByUserId(userId);

        SecurityContextHolder.clearContext();

        log.info("Logout success: id={}", userId);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
