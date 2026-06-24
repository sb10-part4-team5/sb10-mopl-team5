package com.codeit.team5.mopl.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String accessSecretKey,
        String refreshSecretKey,
        long accessTokenExpirationMinutes,
        long refreshTokenExpirationMinutes
) {
}
