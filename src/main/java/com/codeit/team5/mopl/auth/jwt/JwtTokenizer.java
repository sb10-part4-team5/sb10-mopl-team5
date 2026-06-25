package com.codeit.team5.mopl.auth.jwt;

import com.codeit.team5.mopl.auth.exception.JwtInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
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
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(getRefreshKey())
                .build()
                .parseClaimsJws(jws);
        if (!"REFRESH".equals(claimsJws.getBody().get("tokenType", String.class))) {
            throw new JwtInvalidException("Invalid refresh token type");
        }

        return claimsJws;
    }

    private Key getAccessKey() {
        return getKey(jwtProperties.accessSecretKey());
    }

    private Key getRefreshKey() {
        return getKey(jwtProperties.refreshSecretKey());
    }

    private Key getKey(String secretKey) {
        String base64EncodedSecretKey =
                Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));

        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64EncodedSecretKey));
    }

    private Date getTokenExpiration(long expirationMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, Math.toIntExact(expirationMinutes));
        return calendar.getTime();
    }
}
