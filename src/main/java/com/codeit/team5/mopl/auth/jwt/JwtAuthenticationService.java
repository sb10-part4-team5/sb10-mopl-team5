package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.service.AuthSessionService;
import com.codeit.team5.mopl.auth.support.MoplAccountStatusChecker;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// 인증 객체 생성용
@Component
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenizer jwtTokenizer;
    private final JwtPrincipalLoader jwtPrincipalLoader;
    private final AuthSessionService authSessionService;
    private final MoplAccountStatusChecker moplAccountStatusChecker;

    public Authentication getAuthentication(String accessToken) {
        Claims claims = jwtTokenizer.getAccessClaims(accessToken).getBody();

        UUID userId = getSubjectAsUserId(claims);
        UUID sessionId = getSessionId(claims);

        if (!authSessionService.isValidSession(userId, sessionId)) {
            throw new JwtInvalidException("Invalid login session");
        }

        MoplUserDetails principal =
                jwtPrincipalLoader.loadByUserId(userId);

        moplAccountStatusChecker.check(principal);

        return new UsernamePasswordAuthenticationToken(
                principal,
                accessToken,
                principal.getAuthorities()
        );
    }

    private UUID getSessionId(Claims claims) {
        Object sessionId = claims.get("sessionId");

        if (!(sessionId instanceof String value) || value.isBlank()) {
            throw new JwtInvalidException("Invalid token sessionId");
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new JwtInvalidException("Invalid token sessionId", e.getMessage());
        }
    }

    private UUID getSubjectAsUserId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtInvalidException("Invalid token subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtInvalidException("Invalid token subject", e.getMessage());
        }
    }
}
