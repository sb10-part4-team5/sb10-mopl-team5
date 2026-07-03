package com.codeit.team5.mopl.auth.controller;

import com.codeit.team5.mopl.auth.controller.api.AuthApi;
import com.codeit.team5.mopl.auth.dto.request.ResetPasswordRequest;
import com.codeit.team5.mopl.auth.service.PasswordResetService;
import com.codeit.team5.mopl.auth.support.RefreshTokenCookieManager;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.service.AuthService;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieManager cookieManager;

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

    @Override
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset request: POST /api/auth/reset-password");

        passwordResetService.resetPassword(request);

        return ResponseEntity.noContent().build();
    }
}
