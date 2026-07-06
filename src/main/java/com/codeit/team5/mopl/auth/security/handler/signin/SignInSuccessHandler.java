package com.codeit.team5.mopl.auth.security.handler.signin;

import com.codeit.team5.mopl.auth.support.ErrorResponder;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.jwt.JwtProperties;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.auth.mapper.AuthMapper;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignInSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    private final RefreshTokenCookieManager cookieManager;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthMapper authMapper;
    private final JwtTokenizer jwtTokenizer;
    private final JwtProperties jwtProperties;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        MoplPrincipal principal = (MoplPrincipal) authentication.getPrincipal();

        User user = findUserOrSendError(response, principal);
        if (user == null) {
            return;
        }

        UserResponse userDto = userMapper.toDto(user);

        String accessToken = jwtTokenizer.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenizer.generateRefreshToken(user.getId().toString());
        refreshTokenStore.save(user.getId(), refreshToken, calculateExpiresAt());

        ResponseCookie responseCookie = cookieManager.createCookie(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        log.info("Login success");

        JwtResponse jwtResponse = authMapper.toJwtResponse(userDto, accessToken);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), jwtResponse);
    }

    private User findUserOrSendError(HttpServletResponse response, MoplPrincipal principal)
            throws IOException {
        try {
            // 로그인 응답 생성에 필요한 프로필 이미지를 함께 조회
            return userRepository.findWithProfileImageById(principal.getId())
                    .orElseThrow(() -> new UserNotFoundException(principal.getId()));
        } catch (UserNotFoundException e) {
            log.warn("Login success handler failed. userId={}", principal.getId(), e);
            ErrorResponder.sendErrorResponse(response, e);
            return null;
        }
    }

    private Instant calculateExpiresAt() {
        return Instant.now()
                .plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }
}
