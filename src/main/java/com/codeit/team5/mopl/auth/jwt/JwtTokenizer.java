package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenizer {

    private final JwtProperties jwtProperties;

    // subject = userId 사용
    public String generateAccessToken(String subject, String email, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(getTokenExpiration(jwtProperties.accessTokenExpirationMinutes()))
                .signWith(getAccessKey())
                .compact();
    }

    public String generateRefreshToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("tokenType", "REFRESH")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(getTokenExpiration(jwtProperties.refreshTokenExpirationMinutes()))
                .signWith(getRefreshKey())
                .compact();
    }

    public Jws<Claims> getAccessClaims(String jws) {
        return Jwts.parserBuilder()
                .setSigningKey(getAccessKey())
                .build()
                .parseClaimsJws(jws);
    }

    public Jws<Claims> getRefreshClaims(String jws) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(getRefreshKey())
                    .build()
                    .parseClaimsJws(jws);

            if (!"REFRESH".equals(claimsJws.getBody().get("tokenType", String.class))) {
                throw new RefreshTokenInvalidException("Invalid refresh token type");
            }

            return claimsJws;
        } catch (RefreshTokenInvalidException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new RefreshTokenInvalidException("Invalid refresh token");
        }
    }

    public UUID getRefreshUserId(String refreshToken) {
        try {
            Jws<Claims> claimsJws = getRefreshClaims(refreshToken);
            return UUID.fromString(claimsJws.getBody().getSubject());
        } catch (IllegalArgumentException e) {
            throw new RefreshTokenInvalidException("Invalid refresh token subject");
        }
    }

    public Authentication getAuthentication(String accessToken) {
        Jws<Claims> claimsJws = getAccessClaims(accessToken);
        Claims claims = claimsJws.getBody();
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));
        // 로그인에 성공한 인증된 유저이기 때문에 locked=false로 둬도 괜찮음
        AuthUser authUser = new AuthUser(userId, email, role, false);
        // 실제로 인증 객체를 만들어야 하기 때문에 구현 클래스 사용
        UserDetails principal = new MoplUserDetails(authUser, "");
        return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
    }

    private Key getAccessKey() {
        return getKey(jwtProperties.accessSecretKey());
    }

    private Key getRefreshKey() {
        return getKey(jwtProperties.refreshSecretKey());
    }

    private Key getKey(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Date getTokenExpiration(long expirationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, Math.toIntExact(expirationMinutes));
        return calendar.getTime();
    }
}
