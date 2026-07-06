package com.codeit.team5.mopl.auth.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(

        @NotBlank
        String accessSecretKey,

        @NotBlank
        String refreshSecretKey,

        @Min(1)
        long accessTokenExpirationMinutes,

        @Min(1)
        long refreshTokenExpirationMinutes
) {
}
