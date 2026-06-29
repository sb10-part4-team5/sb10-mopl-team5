package com.codeit.team5.mopl.content.client.tmdb;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(

        @NotBlank
        String accessToken,

        @NotBlank
        String baseUrl
) {
}
