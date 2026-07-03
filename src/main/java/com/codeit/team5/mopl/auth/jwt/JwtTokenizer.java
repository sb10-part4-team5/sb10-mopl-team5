package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.RefreshTokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    // accessToken 서명과 만료 검증
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
