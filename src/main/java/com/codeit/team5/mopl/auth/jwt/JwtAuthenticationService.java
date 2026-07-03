package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.AccountLockedException;
import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

// 인증 객체 생성용 클래스
@Component
@RequiredArgsConstructor
public class JwtAuthenticationService {

    private final JwtTokenizer jwtTokenizer;
    private final MoplUserDetailsService userDetailsService;

    public Authentication getAuthentication(String accessToken) {
        Claims claims = jwtTokenizer.getAccessClaims(accessToken).getBody();

        UUID userId = getSubjectAsUserId(claims);

        MoplUserDetails principal =
                (MoplUserDetails) userDetailsService.loadUserById(userId);

        if (!principal.isAccountNonLocked()) {
            throw new AccountLockedException("잠긴 계정입니다.");
        }

        return new UsernamePasswordAuthenticationToken(
                principal,
                accessToken,
                principal.getAuthorities()
        );
    }

    private UUID getSubjectAsUserId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtInvalidException("Invalid token subject");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtInvalidException("Invalid token subject");
        }
    }
}
