package com.codeit.team5.mopl.auth.controller;

import com.codeit.team5.mopl.auth.controller.api.AuthApi;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.service.AuthService;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final RefreshTokenCookieManager cookieManager;

    // 실제 로그인 처리는 Spring Security UsernamePasswordAuthenticationFilter가 담당
    // 이 메서드는 Swagger 문서 노출을 위한 선언
    @PostMapping(
            value = "/sign-in",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<JwtResponse> login(
            @Valid @ModelAttribute SignInRequest request
    ) {
        log.info("Login request: POST /api/auth/sign-in");

        throw new UnsupportedOperationException("Handled by Spring Security filter");
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    ) {
        log.info("Logout request: POST /api/auth/sign-out");

        throw new UnsupportedOperationException("Handled by Spring Security filter");
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    ) {
        log.info("RefreshToken regenerate request: POST /api/auth/refresh");

        AuthPayload authPayload = authService.refresh(refreshToken);

        ResponseCookie refreshTokenCookie =
                cookieManager.createCookie(authPayload.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(authPayload.jwtResponse());
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> csrfToken(CsrfToken csrfToken) {
        log.info("CSRF token request: GET /api/auth/csrf-token");

        csrfToken.getToken();

        return ResponseEntity.noContent().build();
    }
}
