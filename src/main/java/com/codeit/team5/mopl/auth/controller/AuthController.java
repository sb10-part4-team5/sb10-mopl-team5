package com.codeit.team5.mopl.auth.controller;

import com.codeit.team5.mopl.auth.dto.request.SignInRequest;
import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody SignInRequest request
    ) {
        log.info("Login request: POST /api/auth/sign-in, email={}", request.username());
        JwtResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> logout() {
        log.info("Logout request: POST /api/auth/sign-out");
        authService.logout();

        return ResponseEntity.noContent().build();
    }
}
