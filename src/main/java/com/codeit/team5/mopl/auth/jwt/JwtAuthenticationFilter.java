package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.AccountLockedException;
import com.codeit.team5.mopl.auth.exception.AuthException;
import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_EXCEPTION_ATTRIBUTE = "authException";

    private final JwtTokenizer jwtTokenizer;
    private final MoplUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!hasBearerToken(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            Jws<Claims> claimsJws = jwtTokenizer.getAccessClaims(accessToken);
            Claims claims = claimsJws.getBody();

            String email = claims.get("email", String.class);

            if (email == null) {
                throw new JwtInvalidException("Invalid token email");
            }

            MoplUserDetails userDetails = (MoplUserDetails) userDetailsService.loadUserByUsername(email);
            validateSubject(claims, userDetails.getId());

            if (!userDetails.isAccountNonLocked()) {
                throw new AccountLockedException("잠긴 계정입니다");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(email);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException | AuthException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_EXCEPTION_ATTRIBUTE, e);
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasBearerToken(String authorizationHeader) {
        return Objects.nonNull(authorizationHeader)
                && authorizationHeader.startsWith(BEARER_PREFIX);
    }

    private void validateSubject(Claims claims, UUID userId) {
        String subject = claims.getSubject();

        if (subject == null) {
            throw new JwtInvalidException("Invalid token subject");
        }

        try {
            UUID subjectUserId = UUID.fromString(subject);

            if (!subjectUserId.equals(userId)) {
                throw new JwtInvalidException("Invalid token subject");
            }
        } catch (IllegalArgumentException e) {
            throw new JwtInvalidException("Invalid token subject");
        }
    }
}
