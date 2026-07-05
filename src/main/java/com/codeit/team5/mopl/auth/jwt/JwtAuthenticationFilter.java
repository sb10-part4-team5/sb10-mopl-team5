package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.AuthException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_EXCEPTION_ATTRIBUTE = "authException";

    private final JwtAuthenticationService jwtAuthenticationService;

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
            Authentication authentication =
                    jwtAuthenticationService.getAuthentication(accessToken);

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
}
